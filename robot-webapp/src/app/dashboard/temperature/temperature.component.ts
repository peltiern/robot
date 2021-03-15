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
    {data: [], label: 'Température', fill: false}
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
        duration: 10000,
        delay: 1000
      },
    },
  };
  public lineChartColors: Color[] = [
    {
      backgroundColor: 'rgba(148,159,177,0.2)',
      borderColor: 'rgba(148,159,177,1)',
      pointBackgroundColor: [],
      pointBorderColor: '#fff',
      pointHoverBackgroundColor: [],
      pointHoverBorderColor: 'rgba(148,159,177,0.8)'
    }
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
    // Affichage de la température sur 2 décimales
    this.lineChartData[0].data.push(parseFloat(parseFloat(temperatureMsg.temperature).toFixed(2)));
    // Couleurs des points en fonction de la température
    if (temperatureMsg.temperature > 20) {
      this.lineChartData[0].pointBackgroundColor[this.lineChartData[0].data.length - 1] = 'red';
      this.lineChartData[0].pointHoverBackgroundColor[this.lineChartData[0].data.length - 1] = 'red';
    } else {
      this.lineChartData[0].pointBackgroundColor[this.lineChartData[0].data.length - 1] = 'blue';
      this.lineChartData[0].pointHoverBackgroundColor[this.lineChartData[0].data.length - 1] = 'blue';
    }
    this.chart.update();
  }

}
