-- Schéma de la mémoire longue du robot (SQLite).
--
-- Rejoué à chaque démarrage : tout est en CREATE ... IF NOT EXISTS, donc sans effet sur une base
-- déjà en place. C'est ce qui remplace la sérialisation Java de MapDB, où la forme des données
-- n'était écrite nulle part et où ajouter un champ à un record corrompait silencieusement
-- l'existant.

CREATE TABLE IF NOT EXISTS personne (
    id                 TEXT PRIMARY KEY,
    prenom             TEXT NOT NULL,
    -- ISO-8601 local, ou NULL tant que la personne n'a jamais été rencontrée.
    derniere_rencontre TEXT
);

-- Empreintes biométriques SFace. Plusieurs par personne, volontairement : une seule prise de
-- trois quarts ou à contre-jour, et la personne n'est plus jamais reconnue.
CREATE TABLE IF NOT EXISTS visage (
    id          TEXT PRIMARY KEY,
    id_personne TEXT NOT NULL REFERENCES personne(id) ON DELETE CASCADE,
    -- 128 flottants en little-endian, soit 512 octets.
    empreinte   BLOB NOT NULL
);

CREATE INDEX IF NOT EXISTS index_visage_personne ON visage(id_personne);

-- Journal des rencontres : une ligne par rencontre annoncée, jamais mise à jour. C'est ce qui
-- donne la timeline d'apparition d'une personne, que `personne.derniere_rencontre` ne peut pas
-- fournir puisqu'elle est écrasée à chaque fois.
CREATE TABLE IF NOT EXISTS rencontre (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    id_personne       TEXT NOT NULL REFERENCES personne(id) ON DELETE CASCADE,
    instant           TEXT NOT NULL,
    -- ACCUEIL, RETROUVAILLES ou REPRISE : ce que le robot a fait de cette rencontre.
    type              TEXT NOT NULL,
    -- Durée de l'absence qui a précédé, -1 à la toute première apparition.
    secondes_absence  INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS index_rencontre_personne ON rencontre(id_personne, instant);

-- Messages échangés avec l'IA.
--
-- Pas de clé étrangère vers `personne` ici, et c'est délibéré : un fil de conversation n'est pas
-- toujours celui de quelqu'un de connu — `wall-e` (interlocuteur non identifié) et `chauffe`
-- existent aussi. Effacer la conversation d'une personne supprimée est donc à la charge du
-- répertoire, qui sait construire la clé `personne-<id>`.
CREATE TABLE IF NOT EXISTS message (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    id_conversation TEXT    NOT NULL,
    -- Position dans le fil : l'ordre des messages est le sens même d'une conversation, et
    -- l'identifiant auto-incrémenté ne suffirait pas après une réécriture complète du fil.
    rang            INTEGER NOT NULL,
    type            TEXT    NOT NULL,
    texte           TEXT    NOT NULL
);

CREATE INDEX IF NOT EXISTS index_message_conversation ON message(id_conversation, rang);
