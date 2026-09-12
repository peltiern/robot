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
  | 'recharger'
  | 'crayon'
  | 'plus'
  | 'alerte'
  | 'chenille'
  | 'manette'
  | 'annuler'
  | 'retablir'
  | 'debut'
  | 'pause'
  | 'boucle'
  | 'moins'
  | 'toutVoir'
  | 'ecran'
  | 'robot'
  | 'disquette'
  | 'corbeille'
  | 'sablier'

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
  // Flèche qui revient sur elle-même : relire, et non rejouer (cf. `reprise`).
  recharger: (
    <>
      <path d="M20.2 12a8.2 8.2 0 1 1-2.4-5.8" />
      <path d="M20.5 4.2v4.4h-4.4" />
    </>
  ),
  plus: <path d="M12 5.2v13.6M5.2 12h13.6" />,
  // Crayon : corriger ce qui est écrit. Le distinguer de `reglage`, qui ouvre des
  // réglages — un prénom mal compris se rature, il ne se paramètre pas.
  crayon: (
    <>
      <path d="M4.2 15.5 15.6 4.1a2.4 2.4 0 0 1 3.4 0l.9.9a2.4 2.4 0 0 1 0 3.4L8.5 19.8l-5 1.2z" />
      <path d="m14.4 5.3 4.3 4.3" />
    </>
  ),
  alerte: (
    <>
      <path d="M12 3.6 22 20H2z" />
      <path d="M12 9.6v4.6" />
      <circle cx="12" cy="17" r="1.2" fill="currentColor" stroke="none" />
    </>
  ),
  /*
   * Chenille de profil, au galbe de celles de WALL·E : un triangle, petit galet de
   * tension en haut à l'avant, deux gros galets au sol. La silhouette asymétrique est
   * ce qui permet de distinguer les deux côtés — l'icône de la chenille gauche est la
   * même, retournée (cf. `santeIconeMiroir`). Une bande symétrique donnait deux
   * pastilles identiques qu'on ne pouvait plus attribuer.
   */
  chenille: (
    <>
      <path d="M7.28 6.19A2.8 2.8 0 0 1 11.71 5.36L19.53 12.68A4 4 0 0 1 16.8 19.6H7.2A4 4 0 0 1 3.59 13.87Z" />
      <circle cx="9.8" cy="7.4" r="1.3" />
      <circle cx="7.2" cy="15.6" r="2.3" />
      <circle cx="16.8" cy="15.6" r="2.3" />
    </>
  ),
  // Manette : coque à poignées, croix directionnelle à gauche, bouton à droite.
  manette: (
    <>
      <path d="M8.8 7.8h6.4a5.2 5.2 0 0 1 5.1 4.2l.7 3.8a2.5 2.5 0 0 1-4.5 1.9L15 15.6H9l-1.5 2.1a2.5 2.5 0 0 1-4.5-1.9l.7-3.8a5.2 5.2 0 0 1 5.1-4.2Z" />
      <path d="M8 10.8v2.4M6.8 12h2.4" />
      <circle cx="16.2" cy="12" r="1.1" fill="currentColor" stroke="none" />
    </>
  ),
  annuler: (
    <>
      <path d="M9 14 4.5 9.5 9 5" />
      <path d="M4.5 9.5H14a5.5 5.5 0 0 1 0 11h-3" />
    </>
  ),
  retablir: (
    <>
      <path d="m15 14 4.5-4.5L15 5" />
      <path d="M19.5 9.5H10a5.5 5.5 0 0 0 0 11h3" />
    </>
  ),
  debut: (
    <>
      <path d="M6 5.4v13.2" />
      <path d="M18.5 5.4 9.5 12l9 6.6z" />
    </>
  ),
  // Deux barres creuses, comme `reprise` est un triangle creux : les deux se remplacent l'une
  // l'autre sur le même bouton, et ne doivent pas changer de graisse en basculant.
  pause: (
    <>
      <rect x="6.2" y="5.4" width="3.8" height="13.2" rx="1.3" />
      <rect x="14" y="5.4" width="3.8" height="13.2" rx="1.3" />
    </>
  ),
  boucle: (
    <>
      <path d="M4 11.5V10a4 4 0 0 1 4-4h11" />
      <path d="m16 3 3 3-3 3" />
      <path d="M20 12.5V14a4 4 0 0 1-4 4H5" />
      <path d="m8 21-3-3 3-3" />
    </>
  ),
  moins: <path d="M5.2 12h13.6" />,
  // Deux flèches qui s'écartent jusqu'à deux butées : tout ramener entre les bords.
  toutVoir: (
    <>
      <path d="M3.6 5v14M20.4 5v14" />
      <path d="M7.4 12h9.2" />
      <path d="m10 9.2-2.8 2.8 2.8 2.8M14 9.2l2.8 2.8-2.8 2.8" />
    </>
  ),
  ecran: (
    <>
      <rect x="2.8" y="4.2" width="18.4" height="12.4" rx="2.4" />
      <path d="M8.5 20h7M12 16.6V20" />
    </>
  ),
  // Les deux coques de WALL·E, jumelées : le robot lui-même. À ne pas confondre avec `oeil`, qui
  // veut dire « ce qu'il voit ».
  robot: (
    <>
      <path d="M3 9.6a2.8 2.8 0 0 1 2.8-2.8h4.4A1.8 1.8 0 0 1 12 8.6v6.2a3.4 3.4 0 0 1-3.4 3.4H6.6A3.6 3.6 0 0 1 3 14.6z" />
      <path d="M21 9.6a2.8 2.8 0 0 0-2.8-2.8h-4.4A1.8 1.8 0 0 0 12 8.6v6.2a3.4 3.4 0 0 0 3.4 3.4h2a3.6 3.6 0 0 0 3.6-3.6z" />
      <circle cx="7.6" cy="12.4" r="1.4" fill="currentColor" stroke="none" />
      <circle cx="16.4" cy="12.4" r="1.4" fill="currentColor" stroke="none" />
    </>
  ),
  disquette: (
    <>
      <path d="M5.6 3.6h10.2l4.6 4.6v10.2a2 2 0 0 1-2 2H5.6a2 2 0 0 1-2-2V5.6a2 2 0 0 1 2-2Z" />
      <path d="M7.6 3.6v4.6h7V3.6M7.4 20.4v-6h9.2v6" />
    </>
  ),
  corbeille: (
    <>
      <path d="M4 6.6h16M9.4 6.6V4.4h5.2v2.2" />
      <path d="m6 6.6 1 12.6a1.6 1.6 0 0 0 1.6 1.4h6.8a1.6 1.6 0 0 0 1.6-1.4l1-12.6" />
      <path d="M10 10.6v6M14 10.6v6" />
    </>
  ),
  sablier: (
    <>
      <path d="M6.4 3.4h11.2M6.4 20.6h11.2" />
      <path d="M7.6 3.4c0 4.2 4.4 5.4 4.4 8.6s-4.4 4.4-4.4 8.6M16.4 3.4c0 4.2-4.4 5.4-4.4 8.6s4.4 4.4 4.4 8.6" />
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
