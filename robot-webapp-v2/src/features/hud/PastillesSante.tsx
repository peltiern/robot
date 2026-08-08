import { Icone, type NomIcone } from '../../shared/components/Icone'
import { useSanteStore, type SanteOrgane } from '../../shared/sante/santeStore'
import styles from './hud.module.css'

/**
 * Icône par organe, sur les identifiants de `/api/organes`. Un organe absent de cette table
 * retombe sur un rond neutre plutôt que de disparaître : le jour où le robot en expose un
 * nouveau, il apparaît quand même dans la barre — sans icône, mais avec son état.
 *
 * `miroir` retourne le tracé horizontalement. Les deux chenilles partagent forcément la même
 * icône ; c'est leur orientation qui les distingue, comme les deux flancs du robot.
 */
const ICONES: Record<string, { icone: NomIcone; miroir?: boolean }> = {
  cou: { icone: 'posture' },
  yeux: { icone: 'oeil' },
  'chenille-gauche': { icone: 'chenille', miroir: true },
  'chenille-droite': { icone: 'chenille' },
  manette: { icone: 'manette' },
  vision: { icone: 'camera' },
  micro: { icone: 'micro' },
  animation: { icone: 'anim' },
  materiel: { icone: 'jauge' },
}

/**
 * Sépare les deux familles en conservant l'ordre du robot à l'intérieur de chacune.
 *
 * Actionneurs d'abord : ce sont eux qui bougent, donc eux qu'on cherche des yeux quand le robot
 * fait quelque chose d'inattendu. Un capteur muet informe, un actionneur muet inquiète.
 */
function parFamille(organes: SanteOrgane[]) {
  return {
    actionneurs: organes.filter((o) => o.nature === 'ACTIONNEUR'),
    capteurs: organes.filter((o) => o.nature === 'CAPTEUR'),
  }
}

/**
 * État des organes dans la barre du haut, à côté des mesures épinglées : une icône par organe,
 * sans libellé, actionneurs et capteurs en deux groupes séparés.
 *
 * C'est l'inventaire vu d'un coup d'œil, sans avoir à ouvrir le volet Vitaux — le seul endroit
 * où l'on regarde quand le robot vient de couper ses moteurs tout seul. Les libellés sont
 * remplacés par les icônes du HUD : sur une tablette à bout de bras, on reconnaît la forme bien
 * avant de lire « Chenille droite ».
 *
 * Les deux chenilles portent la même icône ; ce qui les distingue est leur ordre, et l'infobulle.
 */
export function IconesSante() {
  const organes = useSanteStore((s) => s.organes)

  if (organes.length === 0) return null

  const { actionneurs, capteurs } = parFamille(organes)

  return (
    <>
      {[actionneurs, capteurs]
        .filter((famille) => famille.length > 0)
        .map((famille) => (
          <div key={famille[0].nature} className={styles.santeBarre}>
            {famille.map((organe) => (
              <IconeOrgane key={organe.id} organe={organe} />
            ))}
          </div>
        ))}
    </>
  )
}

function IconeOrgane({ organe }: { organe: SanteOrgane }) {
  const classes = [styles.santeIcone, styles[`sante${organe.etat}`]]
  if (organe.surveille) classes.push(styles.santeIconeSurveille)

  const dessin = ICONES[organe.id]

  return (
    <span className={classes.join(' ')} title={infobulle(organe)}>
      {dessin ? (
        <Icone
          nom={dessin.icone}
          taille={20}
          className={dessin.miroir ? styles.santeIconeMiroir : undefined}
        />
      ) : (
        <i className={styles.santePoint} />
      )}
    </span>
  )
}

/**
 * Pastilles d'état des organes : une par organe, verte s'il donne signe de vie, rouge s'il se
 * tait, grise s'il est éteint volontairement.
 *
 * C'est l'autre bout du watchdog : lui et ces pastilles lisent le **même** relevé de
 * battements. Quand les moteurs se coupent tout seuls, l'explication est déjà à l'écran — la
 * pastille de l'organe fautif était passée au rouge avant la coupure.
 *
 * Le contour marque les organes qui peuvent déclencher un arrêt d'urgence : leur silence coûte les
 * moteurs, celui d'un capteur ne coûte qu'une pastille rouge.
 */
export function PastillesSante() {
  const organes = useSanteStore((s) => s.organes)

  if (organes.length === 0) return null

  const { actionneurs, capteurs } = parFamille(organes)

  return (
    <div className={styles.sante}>
      <Famille titre="Actionneurs" organes={actionneurs} />
      <Famille titre="Capteurs" organes={capteurs} />
    </div>
  )
}

/**
 * Une famille et son intitulé. L'intitulé n'est pas décoratif : sans lui, « Manette » au milieu
 * des capteurs se lit comme une erreur de rangement — c'en est une seulement si on croit qu'être
 * surveillé par le watchdog fait de vous un actionneur.
 */
function Famille({ titre, organes }: { titre: string; organes: SanteOrgane[] }) {
  if (organes.length === 0) return null

  return (
    <div className={styles.santeFamille}>
      <span className={`${styles.santeFamilleTitre} eyebrow`}>{titre}</span>
      <div className={styles.santeFamilleListe}>
        {organes.map((organe) => (
          <Pastille key={organe.id} organe={organe} />
        ))}
      </div>
    </div>
  )
}

function Pastille({ organe }: { organe: SanteOrgane }) {
  const classes = [styles.santeOrgane, styles[`sante${organe.etat}`]]
  if (organe.surveille) classes.push(styles.santeSurveille)

  return (
    <span className={classes.join(' ')} title={infobulle(organe)}>
      <i className={styles.santePoint} />
      {organe.libelle}
    </span>
  )
}

/**
 * Texte au survol / au toucher. L'âge du dernier battement n'est pas décoratif : c'est lui qui
 * distingue « muet depuis une seconde » (à surveiller) de « muet depuis dix minutes » (mort).
 */
function infobulle(organe: SanteOrgane): string {
  const surveille = organe.surveille ? ' — surveillé par le watchdog' : ''
  if (organe.etat === 'ETEINT') return `${organe.libelle} : éteint${surveille}`
  if (organe.ageMillis == null) return `${organe.libelle} : aucun signe de vie encore${surveille}`
  const etat = organe.etat === 'VIVANT' ? 'actif' : 'sans réponse'
  return `${organe.libelle} : ${etat}, dernier signe il y a ${age(organe.ageMillis)}${surveille}`
}

function age(millis: number): string {
  if (millis < 1000) return `${Math.round(millis)} ms`
  if (millis < 60000) return `${(millis / 1000).toFixed(1)} s`
  return `${Math.round(millis / 60000)} min`
}
