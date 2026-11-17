// Implantation des modules dans la caisse — données et contrôles, partagés par la page et par node.
// Repère du modèle : x vers la droite du robot, y en haut, z vers l'arrière, origine au sol, mm.

// L'intérieur de la caisse : l'intérieur du cube de goRAIL (1109-0024-0288/0336), relevé dans robot-anime.glb.
export const INTERIEUR = { min: [-144, 134, -144], max: [144, 422, 144] };

// Ce qui occupe déjà la caisse, relevé pièce par pièce dans robot-anime.glb (marge de 2 mm).
export const INTERDITS = [
  { id: 'cou', nom: 'Servo du monter et parallélogramme du cou', min: [-36, 350, -62], max: [36, 446, 76] },
  { id: 'epaule_d', nom: 'Servo de flexion de l\'épaule droite', min: [72, 340, 26], max: [144, 422, 72] },
  { id: 'epaule_g', nom: 'Servo de flexion de l\'épaule gauche', min: [-144, 340, 26], max: [-72, 422, 72] },
];

// dims : [longueur, largeur, épaisseur] ; l'épaisseur se mesure depuis la face d'appui.
// modele : le nœud de composants.glb, posé face d'appui en y = 0, longueur selon x.
export const MODULES = {
  batterie: { nom: 'Batterie DCB184 + Smart Dock', cat: 'puissance', masse: 780, dims: [111, 77, 89], modele: 'batterie', approx: true,
    source: 'Dock : STEP goBILDA 3104-0020-0001. Batterie : bloc aux cotes de la fiche DeWalt, aucun modèle libre trouvé.',
    note: 'Le plus lourd : il décide du centre de gravité. La batterie glisse sur le dock dans le sens de la longueur : orienter la glissière vers la porte.' },
  arret: { nom: 'Arrêt d\'urgence', cat: 'puissance', masse: 120, dims: [40, 40, 45], approx: true, dehors: true,
    source: 'Pas encore de référence : champignon Ø40 supposé.',
    note: 'Monté sur le goRAIL arrière du toit, loin du balayage des bras.' },
  bec12: { nom: 'BEC 6 A — 18 → 12 V', cat: 'puissance', masse: 30, dims: [46, 43, 17], modele: 'bec',
    source: 'STEP goBILDA 3119-2829-0106.', note: 'Chauffe en charge : à l\'air libre.' },
  pdb12: { nom: 'PDB XT30 — 12 V', cat: 'puissance', masse: 37, dims: [46, 43, 19], modele: 'pdb',
    source: 'STEP goBILDA 3108-2833-0802.', note: 'Distribue vers les deux DCC1000 et le second BEC.' },
  bec5: { nom: 'BEC 6 A — 12 → 5 V', cat: 'puissance', masse: 30, dims: [46, 43, 17], modele: 'bec',
    source: 'STEP goBILDA 3119-2829-0106.', note: 'Alimente le Jetson.' },
  pdb5: { nom: 'PDB XT30 — 5 V', cat: 'puissance', masse: 37, dims: [46, 43, 19], modele: 'pdb', source: 'STEP goBILDA 3108-2833-0802.', note: '' },
  jetson: { nom: 'Jetson Nano 4 Go', cat: 'calcul', masse: 250, dims: [100, 81, 45], modele: 'jetson',
    source: 'STEP NVIDIA P3450 révision A02 (miroir Waveshare). La B01 a le même contour.',
    note: 'Le modèle fait 30,5 mm sans ventilateur : 15 mm sont réservés au-dessus pour un ventilateur de 40. Ports USB, HDMI, Ethernet et carte SD à garder accessibles.' },
  hub: { nom: 'Hub VINT HUB0000', cat: 'phidgets', masse: 20, dims: [45, 45, 17], modele: 'hub',
    source: 'STEP Phidgets HUB0000_0.', note: 'Relié en USB au Jetson ; un câble VINT vers chaque carte Phidgets.' },
  rcc: { nom: 'RCC1000 — 16 servos', cat: 'phidgets', masse: 45, dims: [81, 68, 35], modele: 'rcc',
    source: 'STEP Phidgets RCC1000_1.',
    note: '13 servos à brancher : 3 du cou, 2 des yeux, 4 par bras. Les câbles montent tous vers le haut. Ses 35 mm d\'épaisseur viennent du bornier d\'alimentation.' },
  dcc_d: { nom: 'DCC1000 — chenille droite', cat: 'phidgets', masse: 60, dims: [93, 79, 33], modele: 'dcc',
    source: 'STEP Phidgets DCC1000_0.', note: 'Radiateur à garder à l\'air.' },
  dcc_g: { nom: 'DCC1000 — chenille gauche', cat: 'phidgets', masse: 60, dims: [93, 79, 33], modele: 'dcc',
    source: 'STEP Phidgets DCC1000_0.', note: 'Radiateur à garder à l\'air.' },
  imu: { nom: 'Centrale inertielle MOT1102', cat: 'capteurs', masse: 10, dims: [41, 30, 17], modele: 'imu',
    source: 'STEP Phidgets MOT1102_1.',
    note: 'À plat, alignée sur les axes du robot, au plus près du centre et loin des courants forts : son magnétomètre en dépend.' },
  hp: { nom: 'Haut-parleur', cat: 'audio', masse: 120, dims: [60, 60, 35], approx: true,
    source: 'Pas encore de référence : volume réservé.', note: 'Contre la paroi pour que le son sorte.' },
  respeaker: { nom: 'ReSpeaker Mic Array v2.0', cat: 'audio', masse: 60, dims: [71, 70, 13], modele: 'respeaker', dehors: true,
    source: 'STEP Seeed Studio.',
    note: 'Hors de la caisse en métal pour entendre à 360°, posé sur les deux plaques 1137 du toit, derrière le cou.' },
};

// Ce que les câbles rejoignent hors des modules.
export const POINTS = {
  moteur_d: { nom: 'moteur 5203 droit', p: [240, 57, 40] },
  moteur_g: { nom: 'moteur 5203 gauche', p: [-240, 57, 40] },
  servo_cou: { nom: 'servo du monter', p: [6, 388, -34] },
  passage_tete: { nom: 'passage vers la tête', p: [0, 446, 0] },
  epaule_d: { nom: 'câbles du bras droit', p: [110, 380, 45] },
  epaule_g: { nom: 'câbles du bras gauche', p: [-110, 380, 45] },
};

export const LIENS = [
  ['puissance', 'batterie', 'arret'], ['puissance', 'arret', 'bec12'], ['puissance', 'bec12', 'pdb12'],
  ['puissance', 'pdb12', 'dcc_d'], ['puissance', 'pdb12', 'dcc_g'], ['puissance', 'pdb12', 'bec5'],
  ['puissance', 'bec5', 'pdb5'], ['puissance', 'pdb5', 'jetson'],
  ['moteur', 'dcc_d', 'moteur_d'], ['moteur', 'dcc_g', 'moteur_g'],
  ['usb', 'jetson', 'hub'], ['usb', 'jetson', 'respeaker'], ['usb', 'jetson', 'passage_tete'], ['usb', 'jetson', 'hp'],
  ['vint', 'hub', 'dcc_d'], ['vint', 'hub', 'dcc_g'], ['vint', 'hub', 'rcc'], ['vint', 'hub', 'imu'],
  ['servo', 'rcc', 'servo_cou'], ['servo', 'rcc', 'passage_tete'], ['servo', 'rcc', 'epaule_d'], ['servo', 'rcc', 'epaule_g'],
];

// Ce qui perturbe un magnétomètre : courants forts et aimants.
const PERTURBATEURS = ['batterie', 'arret', 'bec12', 'pdb12', 'dcc_d', 'dcc_g', 'moteur_d', 'moteur_g', 'hp'];

// Une pose : n, la normale de la face d'appui (le côté composants) ; sur, la cote de cette face selon l'axe de n ;
// long, l'axe de la longueur ; c, la position du centre sur les deux autres axes.
const DEHORS = {
  arret: { n: 'y', sur: 446, long: 'x', c: [-85, 0, 156] },
  respeaker: { n: 'y', sur: 449, long: 'x', c: [5, 0, 110] },
};
const sol = (x, z, sur, long = 'z', extra = {}) => ({ n: 'y', sur, long, c: [x, 0, z], ...extra });
const paroi = (n, sur, y, a, long = 'z', extra = {}) => {
  const c = n.endsWith('x') ? [0, y, a] : [a, y, 0];
  return { n, sur, long, c, ...extra };
};

export const IMPLANTATIONS = {
  etageres: {
    nom: 'Étagères', lettre: 'A',
    resume: 'Trois niveaux de plaques goBILDA : la puissance au plancher, les Phidgets au milieu, le Jetson en haut.',
    plaques: [
      { nom: 'plancher', c: [0, 135.5, 0], t: [288, 3, 288] },
      { nom: 'étagère 1', c: [0, 251.5, 0], t: [280, 3, 280] },
      { nom: 'étagère 2', c: [0, 297.5, 0], t: [280, 3, 280] },
    ],
    modules: {
      batterie: sol(0, 80, 137),
      pdb12: sol(-95, 95, 137, 'x'), bec12: sol(-95, 35, 137),
      bec5: sol(95, 35, 137), pdb5: sol(95, 95, 137, 'x'),
      hp: sol(0, -100, 137, 'x'),
      dcc_d: sol(104.5, -60, 253), dcc_g: sol(-104.5, -60, 253),
      imu: sol(0, -60, 253), hub: sol(0, 80, 253),
      jetson: sol(0, 100, 299, 'x'), rcc: sol(0, -95, 299, 'x'),
    },
    acces: { batterie: 'Par l\'arrière, sous l\'étagère 1', jetson: 'Par l\'arrière, en haut', phidgets: 'Il faut retirer l\'étagère 2' },
    pour: ['Montage le plus simple : tout est à plat sur des plaques à trous.', 'Chaque niveau a un rôle, facile à câbler et à expliquer.'],
    contre: ['Le niveau du milieu est enfermé entre deux plaques : chaleur des DCC1000, accès pénible.', 'Les câbles doivent traverser les plaques : prévoir des passages.'],
  },
  parois: {
    nom: 'Parois', lettre: 'B',
    resume: 'Comme une armoire électrique : chaque module sur la paroi la plus proche de ce qu\'il pilote, le centre laissé à la batterie et aux câbles.',
    plaques: [
      { nom: 'plancher', c: [0, 135.5, 0], t: [288, 3, 288] },
      { nom: 'paroi droite', c: [142.5, 250, 0], t: [3, 220, 280] },
      { nom: 'paroi gauche', c: [-142.5, 250, 0], t: [3, 220, 280] },
      { nom: 'paroi arrière (porte)', c: [0, 270, 142.5], t: [280, 180, 3] },
      { nom: 'paroi avant', c: [0, 240, -142.5], t: [280, 200, 3] },
    ],
    modules: {
      batterie: sol(0, 80, 137),
      dcc_d: paroi('-x', 141, 190, -60), dcc_g: paroi('x', -141, 190, -60),
      pdb12: paroi('x', -141, 170, 90), bec12: paroi('x', -141, 225, 90),
      bec5: paroi('-x', 141, 225, 90), pdb5: paroi('-x', 141, 170, 90),
      jetson: paroi('-z', 141, 285, 0, 'x'), hub: paroi('-z', 141, 280, -95, 'x'),
      rcc: paroi('z', -141, 300, 0, 'x'), hp: paroi('z', -141, 300, -95, 'x'),
      imu: sol(0, -50, 137),
    },
    acces: { batterie: 'Porte arrière, au plancher', jetson: 'Porte arrière : ports et carte SD face à soi', phidgets: 'Paroi latérale à démonter' },
    pour: ['Câbles les plus courts : chaque DCC1000 est contre sa chenille, le RCC1000 sous les servos.', 'Rien d\'empilé : tout refroidit à l\'air, le centre reste libre pour faire passer les câbles.', 'Le Jetson souffle directement par la porte arrière.'],
    contre: ['Demande quatre panneaux entre les montants de goRAIL (plaques goBILDA ou contreplaqué).', 'Monter un module sur une paroi verticale est moins pratique que sur une plaque.'],
  },
  tiroir: {
    nom: 'Tiroir', lettre: 'C',
    resume: 'Les étagères, mais sur un tiroir qui sort par l\'arrière. Restent fixés dans la caisse : le RCC1000 et le haut-parleur, trop liés à ce qui les entoure.',
    plaques: [
      { nom: 'tiroir', c: [0, 137, 2.5], t: [270, 6, 275], tiroir: true },
      { nom: 'pont du tiroir', c: [0, 246.5, 2.5], t: [270, 3, 275], tiroir: true },
      { nom: 'paroi avant', c: [0, 240, -142.5], t: [280, 200, 3] },
    ],
    modules: {
      batterie: sol(0, 82, 140, 'z', { tiroir: true }),
      pdb12: sol(-95, 95, 140, 'x', { tiroir: true }), bec12: sol(-95, 35, 140, 'z', { tiroir: true }),
      bec5: sol(95, 35, 140, 'z', { tiroir: true }), pdb5: sol(95, 95, 140, 'x', { tiroir: true }),
      imu: sol(0, -60, 140, 'z', { tiroir: true }),
      dcc_d: sol(90, -55, 248, 'z', { tiroir: true }), dcc_g: sol(-90, -55, 248, 'z', { tiroir: true }),
      jetson: sol(0, 85, 248, 'x', { tiroir: true }), hub: sol(0, -30, 248, 'x', { tiroir: true }),
      rcc: paroi('z', -141, 300, 0, 'x'), hp: paroi('z', -141, 300, -95, 'x'),
    },
    acces: { batterie: 'Tiroir sorti : tout est à portée', jetson: 'Tiroir sorti', phidgets: 'Tiroir sorti' },
    pour: ['Le meilleur accès : on sort toute l\'électronique sur la table pour la mettre au point.', 'Le centre de gravité reste bas.'],
    contre: ['Le plus de mécanique : glissières, butée, verrou.', 'Il faut pouvoir débrancher en sortant : moteurs, servos (via le RCC fixe), arrêt d\'urgence. Des connecteurs en plus, donc des faux contacts possibles.'],
  },
};

const AXE = { x: 0, y: 1, z: 2 };

// La boîte d'un module : centre c, taille t, plus la pose (n, long) qui sert à orienter le modèle.
export function boite(impl, id) {
  const p = DEHORS[id] || IMPLANTATIONS[impl].modules[id];
  const [L, l, e] = MODULES[id].dims;
  const an = AXE[p.n.slice(-1)], sn = p.n.startsWith('-') ? -1 : 1, al = AXE[p.long];
  const t = [0, 0, 0]; t[an] = e; t[al] = L; t[3 - an - al] = l;
  const c = [...p.c]; c[an] = p.sur + sn * e / 2;
  return { c, t, n: p.n, long: p.long, sur: p.sur, tiroir: !!p.tiroir };
}

const bornes = b => ({ min: b.c.map((v, i) => v - b.t[i] / 2), max: b.c.map((v, i) => v + b.t[i] / 2) });
const chevauche = (a, b) => [0, 1, 2].every(i => a.min[i] < b.max[i] - 0.01 && b.min[i] < a.max[i] - 0.01);
const ecart = (a, b) => Math.hypot(...[0, 1, 2].map(i => Math.max(0, a.min[i] - b.max[i], b.min[i] - a.max[i])));

function position(impl, id) {
  if (POINTS[id]) return POINTS[id].p;
  return boite(impl, id).c;
}

export function controler(impl) {
  const I = IMPLANTATIONS[impl];
  const conflits = [];
  const ids = Object.keys(I.modules);
  const B = Object.fromEntries(ids.map(id => [id, bornes(boite(impl, id))]));
  for (const id of ids) {
    const b = B[id];
    if ([0, 1, 2].some(i => b.min[i] < INTERIEUR.min[i] - 0.01 || b.max[i] > INTERIEUR.max[i] + 0.01))
      conflits.push(`${MODULES[id].nom} dépasse de la caisse`);
    for (const z of INTERDITS) if (chevauche(b, z)) conflits.push(`${MODULES[id].nom} touche : ${z.nom}`);
    for (const p of I.plaques) if (chevauche(b, bornes(p))) conflits.push(`${MODULES[id].nom} traverse ${p.nom}`);
  }
  for (let i = 0; i < ids.length; i++) for (let j = i + 1; j < ids.length; j++)
    if (chevauche(B[ids[i]], B[ids[j]])) conflits.push(`${MODULES[ids[i]].nom} et ${MODULES[ids[j]].nom} se chevauchent`);

  // Câbles : trajet en équerre entre centres, x puis z puis y — une estimation, pas un routage.
  const cables = {};
  for (const [type, a, b] of LIENS) {
    const p = position(impl, a), q = position(impl, b);
    cables[type] = (cables[type] || 0) + Math.abs(p[0] - q[0]) + Math.abs(p[1] - q[1]) + Math.abs(p[2] - q[2]);
  }

  let m = 0, my = 0;
  for (const id of Object.keys(MODULES)) { const c = boite(impl, id).c; m += MODULES[id].masse; my += MODULES[id].masse * c[1]; }

  const imu = bornes(boite(impl, 'imu'));
  let proche = null;
  for (const id of PERTURBATEURS) {
    const b = POINTS[id] ? { min: POINTS[id].p.map(v => v - 18), max: POINTS[id].p.map(v => v + 18) } : bornes(boite(impl, id));
    const d = ecart(imu, b);
    if (!proche || d < proche.d) proche = { id, nom: (MODULES[id] || POINTS[id]).nom, d };
  }
  const centre = imu.min.map((v, i) => (v + imu.max[i]) / 2);
  return { conflits, cables, cdg: my / m, masse: m, imu: proche, imuCentre: Math.hypot(centre[0], centre[2]) };
}
