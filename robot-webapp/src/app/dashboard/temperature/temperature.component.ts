import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {ChartDataSets, ChartOptions, ChartType} from 'chart.js';
import {BaseChartDirective, Color, Label} from 'ng2-charts';
import {TemperatureService} from '../../service/temperature.service';
import {Message} from '@stomp/stompjs';
import 'chartjs-plugin-streaming';

@Component({
  selector: 'app-temperature',
  templateUrl: './temperature.component.html',
  styleUrls: ['./temperature.component.css']
})
export class TemperatureComponent implements OnInit, OnDestroy {

// Options du graphique
  public lineChartData: ChartDataSets[] = [
    {data: [], label: 'Température 1', fill: false},
    {data: [], label: 'Température 2', fill: false}
  ];
  public lineChartLabels: Label[] = [];
  public lineChartOptions: (ChartOptions) = {
    responsive: true,
    scales: {
      xAxes: [{
        type: 'realtime',
      }],
      yAxes: [
        {
          position: 'left',
          ticks: {
            min: 10,
            max: 30
          },
          scaleLabel: {
            display: true,
            labelString: 'Temperature'
          }
        }
      ]
    },
    tooltips: {
      mode: 'nearest',
      intersect: false
    },
    hover: {
      mode: 'nearest',
      intersect: false
    },
    plugins: {
      // Options pour le streaming
      streaming: {
        duration: 20000, // Durée de la fenêtre de données en millisecondes (20 secondes dans cet exemple)
        refresh: 1000, // Fréquence de mise à jour du graphique en millisecondes (1 seconde dans cet exemple)
        delay: 2000, // Délai avant le démarrage de l'animation du graphique en millisecondes (2 secondes dans cet exemple)
      },
    },
    animation: {
      duration: 0 // Désactiver les animations pour éviter la mise à jour saccadée du graphique
    }
  };
  public lineChartColors: Color[] = [
    { borderColor: 'blue', backgroundColor: 'rgba(0, 0, 255, 0.2)' },
    { borderColor: 'green', backgroundColor: 'rgba(0, 255, 0, 0.2)' }
  ];
  public lineChartLegend = false;
  public lineChartType: ChartType = 'line';

  // Abonnement au topic "temperature" du Websocket
  public topicSubscription;

  @ViewChild(BaseChartDirective, {static: true}) chart: BaseChartDirective;

  constructor(private temperatureService: TemperatureService) {
  }

  ngOnInit(): void {
    // Ecoute du topic "temperature" du Websocket
    this.topicSubscription = this.temperatureService.getMessageObservable()
      .subscribe((message: Message) => this.onNewTemperatureMsg(message));
  }

  ngOnDestroy(): void {
    this.topicSubscription.unsubscribe();
  }

  /**
   * Met à jour le graphique à partir du message reçu dans le Websocket.
   * @param message le message reçu dans le Websocket
   */
  private onNewTemperatureMsg(message: Message): void {
    const temperatureMsg = JSON.parse(message.body);
    this.lineChartLabels.push(temperatureMsg.time);
    // if (this.lineChartLabels.length > 10) {
    //     this.lineChartLabels.splice(0, 1);
    //     this.lineChartData[0].data.splice(0, 1);
    //   }
    let indexChart = 0;
    if (temperatureMsg.type === 'TYPE_2') {
      indexChart = 1;
    }
    // Affichage de la température sur 2 décimales
    this.lineChartData[indexChart].data.push(parseFloat(parseFloat(temperatureMsg.temperature).toFixed(2)));
    // // Couleurs des points en fonction de la température
    // if (temperatureMsg.temperature > 20) {
    //   this.lineChartData[indexChart].pointBackgroundColor[this.lineChartData[indexChart].data.length - 1] = 'red';
    //   this.lineChartData[indexChart].pointHoverBackgroundColor[this.lineChartData[indexChart].data.length - 1] = 'red';
    // } else {
    //   this.lineChartData[indexChart].pointBackgroundColor[this.lineChartData[indexChart].data.length - 1] = 'blue';
    //   this.lineChartData[indexChart].pointHoverBackgroundColor[this.lineChartData[indexChart].data.length - 1] = 'blue';
    // }
    this.chart.update();
  }

}
