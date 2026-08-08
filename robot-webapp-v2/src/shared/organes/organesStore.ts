import { create } from 'zustand'
import { useTelemetryStore } from '../telemetry/telemetryStore'
import { useSanteStore, type EtatSante } from '../sante/santeStore'

export type Orientation = 'VERTICAL' | 'HORIZONTAL' | 'ROTATION'

/** Articulation pilotable d'un actionneur : butées réelles + position courante. */
export interface Articulation {
  id: string
  libelle: string
  unite: string
  min: number
  max: number
  orientation: Orientation
  position: number | null
}

/** Mesure exposée par un capteur : échelle réelle + dernière valeur connue. */
export interface Mesure {
  id: string
  libelle: string
  unite: string
  min: number
  max: number
  valeur: number | null
}

/** État vital d'un organe, tel que le renvoie la découverte de capacités. */
export interface SanteOrganeRest {
  etat: EtatSante
  ageMillis: number | null
  surveille: boolean
}

export interface Organe {
  id: string
  libelle: string
  type: 'ACTIONNEUR' | 'CAPTEUR'
  articulations: Articulation[]
  mesures: Mesure[]
  /** Absent pour un organe que le robot ne surveille pas. */
  sante?: SanteOrganeRest | null
}

export type EtatChargement = 'chargement' | 'pret' | 'erreur'

interface OrganesState {
  organes: Organe[]
  etat: EtatChargement
  charger: () => void
}

/**
 * Découverte de capacités du robot (`GET /api/organes`), partagée par tout le HUD.
 *
 * Le robot se décrit lui-même : ni la liste des organes, ni les libellés, ni les
 * butées, ni les échelles de mesure ne sont codés en dur côté front. Les volets
 * Posture et Vitaux bouclent simplement sur ce qui est reçu — un capteur ajouté
 * côté robot (tension batterie, courant moteur…) apparaîtrait ici sans une ligne
 * de front à écrire.
 *
 * Une seule requête pour toute l'application (deux volets s'en servent), relancée
 * à chaque (re)connexion et à la demande.
 */
export const useOrganesStore = create<OrganesState>((set) => ({
  organes: [],
  etat: 'chargement',

  charger() {
    set({ etat: 'chargement' })
    fetch('/api/organes')
      .then((r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`)
        return r.json() as Promise<Organe[]>
      })
      .then((organes) => {
        // Amorce la télémétrie UNIQUEMENT pour les mesures sans historique : si le
        // flux live tourne déjà depuis un moment (TelemetryProvider est monté dès
        // le lancement), on n'écrase pas l'historique accumulé avec cet instantané.
        const { historique, ingerer } = useTelemetryStore.getState()
        const aAmorcer: Record<string, number> = {}
        for (const organe of organes) {
          for (const mesure of organe.mesures ?? []) {
            if (mesure.valeur != null && !historique[mesure.id]?.length) {
              aAmorcer[mesure.id] = mesure.valeur
            }
          }
        }
        if (Object.keys(aAmorcer).length > 0) ingerer([aAmorcer])

        // Amorce des pastilles d'état, pour ne pas les laisser vides jusqu'à la première
        // diffusion de `/events/sante-organes` (une seconde plus tard).
        // `nature` vient du type de l'organe : la ressource REST le porte déjà, inutile de le
        // répéter dans sa santé. Les deux sources s'accordent, le robot les tire du même endroit.
        const sante = organes
          .filter((organe) => organe.sante != null)
          .map((organe) => ({
            id: organe.id,
            libelle: organe.libelle,
            nature: organe.type,
            ...organe.sante!,
          }))
        if (sante.length > 0) useSanteStore.getState().appliquer(sante)

        set({ organes, etat: 'pret' })
      })
      .catch(() => set({ etat: 'erreur' }))
  },
}))

/** Actionneurs, filtrés sur les articulations effectivement pilotables. */
export function actionneurs(organes: Organe[], pilotables: (id: string) => boolean): Organe[] {
  return organes
    .filter((o) => o.type === 'ACTIONNEUR')
    .map((o) => ({ ...o, articulations: o.articulations.filter((a) => pilotables(a.id)) }))
    .filter((o) => o.articulations.length > 0)
}

/** Capteurs exposant au moins une mesure. */
export function capteurs(organes: Organe[]): Organe[] {
  return organes.filter((o) => o.type === 'CAPTEUR' && (o.mesures?.length ?? 0) > 0)
}
