import type { ReactNode } from 'react'

/**
 * Jeu d'icônes du HUD, dessinées à la main sur le même gabarit : boîte 24, trait
 * 2,2 px, bouts et jonctions ronds. Deux raisons de ne pas prendre une
 * bibliothèque : les formes rondes et un peu grasses font l'essentiel de
 * l'ambiance cartoon (la moitié de ces icônes — œil, cou, thermomètre — n'existe
 * de toute façon nulle part), et l'appli tourne sur une tablette embarquée sans
 * réseau, où chaque dépendance est du poids en plus.
 *
 * Le tracé hérite de `currentColor` : la couleur se règle en CSS sur le parent.
 */
export type NomIcone =
  | 'oeil'
  | 'camera'
  | 'posture'
  | 'bulle'
  | 'son'
  | 'sonCoupe'
  | 'jauge'
  | 'reglage'
  | 'anim'
  | 'thermo'
  | 'micro'
  | 'stop'
  | 'cible'
  | 'croix'
  | 'visage'
  | 'objet'
  | 'reprise'
  | 'alerte'

const TRACES: Record<NomIcone, ReactNode> = {
  oeil: (
    <>
      <path d="M2.5 12c2.6-4.2 5.8-6.3 9.5-6.3S18.9 7.8 21.5 12c-2.6 4.2-5.8 6.3-9.5 6.3S5.1 16.2 2.5 12Z" />
      <circle cx="12" cy="12" r="3.1" fill="currentColor" stroke="none" />
    </>
  ),
  camera: (
    <>
      <rect x="2.6" y="6.4" width="13.6" height="11.2" rx="3" />
      <path d="M16.2 11.2 21.4 8v8l-5.2-3.2z" />
    </>
  ),
  posture: (
    <>
      <circle cx="12" cy="12" r="4.2" />
      <path d="M12 2.6v3.4M12 18v3.4M2.6 12H6M18 12h3.4" />
    </>
  ),
  bulle: (
    <path d="M4 7.5A3.5 3.5 0 0 1 7.5 4h9A3.5 3.5 0 0 1 20 7.5v5a3.5 3.5 0 0 1-3.5 3.5H10l-4.6 3.6a.6.6 0 0 1-1-.47V16H7.5" />
  ),
  son: (
    <>
      <path d="M4 9.2h3.6L12.5 5v14l-4.9-4.2H4z" />
      <path d="M16.4 9.4a3.7 3.7 0 0 1 0 5.2M19.2 6.8a7.4 7.4 0 0 1 0 10.4" />
    </>
  ),
  sonCoupe: (
    <>
      <path d="M4 9.2h3.6L12.5 5v14l-4.9-4.2H4z" />
      <path d="m16.4 9.8 4.6 4.6M21 9.8l-4.6 4.6" />
    </>
  ),
  jauge: (
    <>
      <path d="M3.6 18.4a9.4 9.4 0 1 1 16.8 0" />
      <path d="M12 18.4 15.8 12" />
      <circle cx="12" cy="18.4" r="1.5" fill="currentColor" stroke="none" />
    </>
  ),
  reglage: (
    <>
      <circle cx="12" cy="12" r="3.3" />
      <path d="M12 2.8v2.6M12 18.6v2.6M4.5 4.5l1.9 1.9M17.6 17.6l1.9 1.9M2.8 12h2.6M18.6 12h2.6M19.5 4.5l-1.9 1.9M6.4 17.6l-1.9 1.9" />
    </>
  ),
  anim: (
    <>
      <rect x="2.8" y="5.5" width="18.4" height="13" rx="2.6" />
      <path d="M8 5.5v13M16 5.5v13" />
      <path d="m12 9.4 2.2 2.6L12 14.6 9.8 12z" fill="currentColor" stroke="none" />
    </>
  ),
  thermo: <path d="M10 13.6V5.4a2 2 0 0 1 4 0v8.2a4 4 0 1 1-4 0Z" />,
  micro: (
    <>
      <rect x="9" y="2.8" width="6" height="10.4" rx="3" />
      <path d="M5.5 11.4a6.5 6.5 0 0 0 13 0M12 17.9v3.3" />
    </>
  ),
  stop: <rect x="5" y="5" width="14" height="14" rx="3" fill="currentColor" stroke="none" />,
  cible: (
    <>
      <circle cx="12" cy="12" r="7.6" />
      <circle cx="12" cy="12" r="2" fill="currentColor" stroke="none" />
      <path d="M12 2.8v2.2M12 19v2.2M2.8 12H5M19 12h2.2" />
    </>
  ),
  croix: <path d="m6 6 12 12M18 6 6 18" />,
  visage: (
    <>
      <circle cx="12" cy="12" r="9.2" />
      <circle cx="9" cy="10.2" r="1.25" fill="currentColor" stroke="none" />
      <circle cx="15" cy="10.2" r="1.25" fill="currentColor" stroke="none" />
      <path d="M8.3 14.5c1 1.2 2.2 1.8 3.7 1.8s2.7-.6 3.7-1.8" />
    </>
  ),
  objet: (
    <>
      <path d="M12 2.9 20.3 7.4v9.2L12 21.1 3.7 16.6V7.4z" />
      <path d="m3.9 7.5 8.1 4.4 8.1-4.4M12 11.9v9.2" />
    </>
  ),
  reprise: <path d="M7.5 5.4 19 12 7.5 18.6z" />,
  alerte: (
    <>
      <path d="M12 3.6 22 20H2z" />
      <path d="M12 9.6v4.6" />
      <circle cx="12" cy="17" r="1.2" fill="currentColor" stroke="none" />
    </>
  ),
}

interface IconeProps {
  nom: NomIcone
  /** Côté du carré, en pixels (le tracé s'épaissit avec, il est vectoriel). */
  taille?: number
  className?: string
}

export function Icone({ nom, taille = 26, className }: IconeProps) {
  return (
    <svg
      className={className}
      viewBox="0 0 24 24"
      width={taille}
      height={taille}
      fill="none"
      stroke="currentColor"
      strokeWidth={2.2}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      {TRACES[nom]}
    </svg>
  )
}
