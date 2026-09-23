import { couleurTimbre } from '../dessin'
import { useStudioStore } from '../store/studioStore'
import { baseHz, borner, TIMBRES, volumes, type Attaque, type Morceau, type Timbre, type Voyelle } from '../synthese/types'
import styles from './PanneauMorceau.module.css'

/**
 * Le panneau du morceau choisi : ce qui se règle mieux au doigt qu'à la souris sur la frise.
 *
 * Il ne dit rien qui ne soit visible sur la frise — il le rend seulement atteignable : le timbre en
 * tuiles dessinées, la bouche en formes plutôt qu'en lettres, et des retouches de forme qu'aucun
 * geste ne ferait bien (aplatir, retourner, égaliser le volume).
 */

/** Icônes des timbres : chacune dessine ce qu'on entend. Gabarit 36 × 20. */
const TRACES: Record<Timbre, string> = {
  voix: 'M2 10 Q6 2 10 10 T18 10 T26 10 T34 10',
  sweep: 'M2 16 Q20 16 34 4',
  note: 'M2 15 H10 V10 H18 V4 H26 V12 H34',
  trill: 'M2 10 L5 5 L8 15 L11 5 L14 15 L17 5 L20 15 L23 5 L26 15 L29 5 L32 15 L34 10',
  warble: 'M2 10 Q4 4 6 10 T10 10 T14 10 T18 10 T22 10 T26 10 T30 10 T34 10',
  blat: 'M2 14 L8 7 V14 L14 7 V14 L20 7 V14 L26 7 V14 L32 7 V14',
}

/** Les bouches, en demi-axes d'ellipse : ouverte pour le « a », étirée pour le « i ». */
const BOUCHES: Record<Voyelle, [number, number]> = {
  a: [9, 7],
  e: [10, 4],
  i: [12, 2],
  o: [6, 6.5],
  u: [3.5, 4],
  y: [5, 3],
}

const ATTAQUES: { valeur: Attaque; libelle: string }[] = [
  { valeur: '', libelle: 'douce' },
  { valeur: 'click', libelle: 'clic' },
  { valeur: 'hiss', libelle: 'souffle' },
]

export function PanneauMorceau({ onEcouter }: { onEcouter: (m: Morceau) => void }) {
  const son = useStudioStore((s) => s.son)
  const selId = useStudioStore((s) => s.selId)
  const modifier = useStudioStore((s) => s.modifier)
  const supprimer = useStudioStore((s) => s.supprimerChoisi)
  const dupliquer = useStudioStore((s) => s.dupliquerChoisi)
  const changerNbPoints = useStudioStore((s) => s.changerNbPoints)

  const choisi = son.morceaux.find((m) => m.id === selId) ?? null

  if (!choisi) {
    return (
      <aside className={styles.panneau}>
        <h2>Morceau</h2>
        <p className={styles.vide}>Touche un morceau de la frise pour le régler.</p>
        <ul className={styles.gestes}>
          <li>
            <b>Glisser un morceau</b> : le déplacer dans le temps, ou le monter et le descendre.
          </li>
          <li>
            <b>Tirer un bord</b> : l’allonger, le raccourcir.
          </li>
          <li>
            <b>Tirer un rond</b> : changer l’intonation.
          </li>
          <li>
            <b>Tirer sa pastille</b> : plus fort vers le haut, plus doux vers le bas — le ruban s’épaissit ou s’affine.
          </li>
          <li>
            <b>Double-toucher</b> un morceau pour ajouter un point, ou le vide pour ajouter un morceau.
          </li>
          <li>
            <b>Le crayon</b> : dessiner le son à main levée — une montée finit en question, une descente en soupir.
          </li>
        </ul>
      </aside>
    )
  }

  const rang = son.morceaux.indexOf(choisi) + 1
  const retoucher = (retouche: (m: Morceau) => void) =>
    modifier((s) => {
      const m = s.morceaux.find((x) => x.id === choisi.id)
      if (m) retouche(m)
    })

  return (
    <aside className={styles.panneau}>
      <div className={styles.entete}>
        <span className={styles.puceTimbre} style={{ background: couleurTimbre(choisi.timbre) }} />
        <strong>Morceau {rang}</strong>
        <button className={styles.mini} title="Écouter ce morceau" onClick={() => onEcouter(choisi)}>
          ▶
        </button>
        <button className={styles.mini} title="Dupliquer" onClick={dupliquer}>
          ⧉
        </button>
        <button className={styles.mini} title="Supprimer (Suppr)" onClick={supprimer}>
          🗑
        </button>
      </div>

      <section className={styles.bloc}>
        <h2>Timbre</h2>
        <div className={styles.tuiles}>
          {(Object.keys(TIMBRES) as Timbre[]).map((timbre) => (
            <button
              key={timbre}
              className={styles.tuile}
              aria-pressed={timbre === choisi.timbre}
              title={TIMBRES[timbre].aide}
              style={{ '--c': couleurTimbre(timbre) } as React.CSSProperties}
              onClick={() =>
                retoucher((m) => {
                  // En changeant de famille (voix ↔ bip), on garde la hauteur entendue et non le
                  // rapport : les bases ne sont pas les mêmes, 480 Hz contre 900.
                  const facteur = baseHz(m.timbre) / baseHz(timbre)
                  m.courbe = m.courbe.map((r) => borner(r * facteur, 0.3, 3.5))
                  m.timbre = timbre
                })
              }
            >
              <svg viewBox="0 0 36 20">
                <path d={TRACES[timbre]} />
              </svg>
              {TIMBRES[timbre].nom}
            </button>
          ))}
        </div>
        <p className={styles.aide}>{TIMBRES[choisi.timbre].aide}</p>
      </section>

      <section className={styles.bloc}>
        <h2>Forme</h2>
        <p className={styles.ligne}>
          durée <b>{(choisi.duree / son.reglages.debit).toFixed(2).replace('.', ',')} s</b>
        </p>
        <div className={styles.points}>
          <button className={styles.mini} onClick={() => changerNbPoints(choisi.courbe.length - 1)}>
            −
          </button>
          <span>
            {choisi.courbe.length} point{choisi.courbe.length > 1 ? 's' : ''}
          </span>
          <button className={styles.mini} onClick={() => changerNbPoints(choisi.courbe.length + 1)}>
            +
          </button>
        </div>
        <div className={styles.ligne}>
          <button
            className={styles.mini}
            title="Toutes les hauteurs à la moyenne"
            onClick={() =>
              retoucher((m) => {
                const moyenne = m.courbe.reduce((a, b) => a + b) / m.courbe.length
                m.courbe = m.courbe.map(() => moyenne)
              })
            }
          >
            Aplatir
          </button>
          <button className={styles.mini} title="Le morceau à l’envers dans le temps" onClick={() => retoucher((m) => {
            m.courbe.reverse()
            volumes(m).reverse()
          })}>
            Retourner
          </button>
          <button
            className={styles.mini}
            title="L’intonation renversée : ce qui montait descend"
            onClick={() =>
              retoucher((m) => {
                const pivot = Math.sqrt(Math.max(...m.courbe) * Math.min(...m.courbe))
                m.courbe = m.courbe.map((r) => borner((pivot * pivot) / r, 0.3, 3.5))
              })
            }
          >
            À l’envers
          </button>
        </div>
        <div className={styles.ligne}>
          <button
            className={styles.mini}
            title="Tous les points au même volume"
            onClick={() =>
              retoucher((m) => {
                const v = volumes(m)
                const moyenne = v.reduce((a, b) => a + b) / v.length
                m.volumes = v.map(() => moyenne)
              })
            }
          >
            Égaliser le volume
          </button>
        </div>
      </section>

      {choisi.timbre === 'voix' && (
        <>
          <section className={styles.bloc}>
            <h2>Bouche</h2>
            <p className={styles.sous}>au début</p>
            <Bouches choisie={choisi.v1} onChoisir={(v) => retoucher((m) => void (m.v1 = v))} />
            <p className={styles.sous}>à la fin</p>
            <Bouches choisie={choisi.v2} onChoisir={(v) => retoucher((m) => void (m.v2 = v))} />
          </section>

          <section className={styles.bloc}>
            <h2>Tremblement</h2>
            <label className={styles.ligne}>
              aucun
              <input
                type="range"
                min={0}
                max={0.08}
                step={0.002}
                value={choisi.vib[1]}
                onChange={(e) => retoucher((m) => void (m.vib[1] = +e.target.value))}
                onPointerUp={() => onEcouter(choisi)}
              />
              fort
            </label>
            <label className={styles.ligne}>
              lent
              <input
                type="range"
                min={2}
                max={24}
                step={0.5}
                value={choisi.vib[0]}
                onChange={(e) => retoucher((m) => void (m.vib[0] = +e.target.value))}
                onPointerUp={() => onEcouter(choisi)}
              />
              rapide
            </label>
          </section>
        </>
      )}

      <section className={styles.bloc}>
        <h2>Attaque</h2>
        <div className={styles.segmente}>
          {ATTAQUES.map(({ valeur, libelle }) => (
            <button
              key={libelle}
              aria-pressed={choisi.attaque === valeur}
              onClick={() => {
                retoucher((m) => void (m.attaque = valeur))
                onEcouter(choisi)
              }}
            >
              {libelle}
            </button>
          ))}
        </div>
      </section>
    </aside>
  )
}

function Bouches({ choisie, onChoisir }: { choisie: Voyelle; onChoisir: (v: Voyelle) => void }) {
  return (
    <div className={styles.bouches}>
      {(Object.keys(BOUCHES) as Voyelle[]).map((v) => (
        <button key={v} className={styles.bouche} aria-pressed={v === choisie} title={`bouche en « ${v} »`} onClick={() => onChoisir(v)}>
          <svg viewBox="0 0 32 20">
            <ellipse cx="16" cy="10" rx={BOUCHES[v][0]} ry={BOUCHES[v][1]} />
          </svg>
          {v}
        </button>
      ))}
    </div>
  )
}
