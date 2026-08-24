SubStack — Résumé du projet

SubStack est une application desktop GUI, open source et 100 % gratuite, destinée à rendre extrêmement simple la fusion de deux pistes de sous-titres en une seule piste ASS bilingue et stylisée.

L'application cible principalement des utilisateurs non techniques. Aucun terminal, aucune commande FFmpeg/MKVToolNix et aucune configuration de PATH ne doivent être nécessaires.

Le workflow doit être :

Glisser-déposer → sélectionner les pistes → configurer le style → choisir la sortie → lancer.

Stack envisagée : Java + JavaFX, avec une architecture propre et multiplateforme.

Les dépendances multimédia nécessaires devront idéalement être embarquées dans les builds afin que l'utilisateur n'ait rien à installer manuellement.

V0 — Fonctionnalité principale

La V0 se concentre sur la méthode d'alignement actuellement validée avec un script prototype.

L'utilisateur sélectionne deux pistes :

Piste principale
Piste secondaire

La piste principale est la timeline de référence.

Pour chaque sous-titre de la piste principale, SubStack recherche le sous-titre correspondant dans la piste secondaire en utilisant principalement le chevauchement temporel.

La V0 doit reproduire le comportement général du prototype présenté plus bas, mais le prototype n'est pas l'architecture à reproduire. Il sert uniquement à définir et valider le comportement attendu.

L'implémentation réelle doit être adaptée à Java/JavaFX et à une architecture propre : UI, logique métier, parsing, génération ASS, traitement vidéo, etc. doivent être correctement séparés.

Entrées

L'utilisateur peut glisser-déposer :

un fichier .mkv
plusieurs fichiers .mkv
un fichier .mp4
plusieurs fichiers .mp4
un ou plusieurs dossiers

Les dossiers doivent être parcourus récursivement, y compris leurs sous-dossiers.

Exemple :

La.Casa.De.Papel/
├── S01/
│   ├── Episode01.mkv
│   ├── Episode02.mkv
│   └── ...
├── S02/
│   └── ...
└── S03/
    └── ...

L'utilisateur doit pouvoir déposer directement le dossier de la série.

Traitement de plusieurs fichiers

Si plusieurs fichiers sont sélectionnés, SubStack doit vérifier qu'ils possèdent une structure de sous-titres compatible.

L'objectif est que l'utilisateur puisse configurer les pistes et le style une seule fois pour tous les fichiers.

Les fichiers doivent notamment avoir des pistes compatibles, avec des noms/titres permettant d'identifier correctement les mêmes pistes.

Si la sélection contient des fichiers incompatibles, SubStack ne doit pas essayer de deviner.

Afficher un message clair, par exemple :

Les fichiers sélectionnés ne possèdent pas une structure de sous-titres compatible. Veuillez traiter ces fichiers individuellement.

C'est particulièrement important pour le traitement de saisons complètes.

Sélection des pistes

L'interface doit afficher clairement les pistes de sous-titres disponibles.

Exemple :

Piste principale
[ 🇪🇸 Spanish - SDH ▼ ]

Piste secondaire
[ 🇫🇷 French - Complets ▼ ]

La piste principale sert de référence temporelle.

L'utilisateur peut choisir librement quelle langue/piste est principale et laquelle est secondaire.

Sortie MKV

Pour un fichier MKV, proposer deux options :

Intégrer la piste dans le MKV

La piste ASS générée est directement intégrée au fichier MKV.

Le traitement doit être un remux, sans réencoder inutilement la vidéo ou l'audio.

Créer un ASS externe

Créer :

NomDeLaVideo.ass

à côté de la vidéo.

Le fichier vidéo original ne doit pas être écrasé ou supprimé sans action explicite de l'utilisateur.

Sortie MP4

SubStack génère des sous-titres ASS, car ce format permet de conserver les styles avancés utilisés par l'application : couleurs, tailles différentes, italique, positionnement, etc.

Le conteneur MP4 ne permet pas d'intégrer directement une piste ASS comme piste de sous-titres native, contrairement au MKV.

Lorsqu'un MP4 est ajouté, SubStack affiche donc un dialogue simple proposant deux choix :

Remuxer en MKV

Le MP4 est remuxé en MKV sans réencoder inutilement la vidéo/audio, puis la piste ASS est intégrée directement au nouveau MKV.

Créer un ASS externe

Créer :

NomDeLaVideo.ass

à côté du MP4.

Aucune connaissance technique ne doit être nécessaire pour comprendre ces options.

Personnalisation des sous-titres

Les deux pistes doivent être configurables indépendamment.

Paramètres minimum :

police ;
taille ;
couleur ;
italique ;
gras ;
position.

La configuration par défaut est orientée apprentissage d'une langue.

Exemple :

Français :
- plus petit
- blanc
- italique

Espagnol :
- plus grand
- jaune
- normal

L'espagnol est la langue cible et doit donc être visuellement dominant.

Rendu souhaité :

        Tu es seule ?
        ¿Estás sola?

Le français sert d'aide secondaire tandis que l'espagnol attire naturellement l'attention.

Positionnement

L'utilisateur peut choisir entre trois dispositions.

Les deux en bas
        Français
        Espagnol

Le secondaire est au-dessus et le principal en dessous.

Les deux en haut

Même principe, mais les deux pistes sont positionnées en haut de l'écran.

Une piste en haut + une piste en bas

Chaque piste est positionnée dans une zone différente de l'écran.

Ces trois modes doivent être accessibles simplement depuis l'interface.

Algorithme V0

La piste principale est la source de vérité temporelle.

Pour chaque sous-titre principal :

rechercher les sous-titres secondaires qui chevauchent son timing ;
sélectionner le meilleur candidat selon le chevauchement temporel ;
éviter qu'un même sous-titre secondaire soit réutilisé plusieurs fois dans les cas simples ;
générer le dialogue ASS final.

Exemple :

Espagnol :
03:10.885 → 03:12.005
"Mamá, ¿estás sola?"

Français :
03:10.925 → 03:13.085
"- Maman, tu es seule ?
 - Oui."

Résultat :

Français :
Maman, tu es seule ?
Oui.

Espagnol :
Mamá, ¿estás sola?

La timeline principale reste la référence.

La V0 doit notamment éviter le problème rencontré lors des premiers tests, où un même sous-titre secondaire pouvait être associé plusieurs fois à plusieurs sous-titres principaux.

Script prototype de référence

Avant de développer SubStack, la fonctionnalité a été testée et validée avec le script Python ci-dessous.

Ce script a servi à vérifier concrètement :

l'extraction des pistes ;
le parsing SRT ;
la correspondance temporelle ;
la génération ASS ;
le style des deux langues ;
leur positionnement ;
le traitement de plusieurs fichiers.

Important : ce script est uniquement un prototype de référence fonctionnelle.

Il ne faut pas le reproduire tel quel dans SubStack.

L'application est une GUI Java/JavaFX et son implémentation doit être adaptée à cette stack et conçue proprement. Le script sert à communiquer au développeur/LLM le comportement attendu de l'algorithme V0, pas à imposer une architecture Python/bash.

Le prototype actuel :

for FILE in *.mkv; do
    echo
    echo "=== $FILE ==="

    ffmpeg -y -i "$FILE" -map 0:4 fr.srt -map 0:5 es.srt >/dev/null 2>&1 || {
        echo "ERREUR extraction : $FILE"
        rm -f fr.srt es.srt
        continue
    }

    python3 - "$FILE" <<'PY'
import re
import sys
from pathlib import Path

file = Path(sys.argv[1])
output = file.with_suffix(".ass")


def parse_srt(filename):
    with open(filename, encoding="utf-8-sig") as f:
        text = f.read().replace("\r\n", "\n")

    entries = []

    for block in re.split(r"\n\s*\n", text.strip()):
        lines = block.split("\n")

        if len(lines) < 3:
            continue

        match = re.match(
            r"(\d+:\d+:\d+,\d+)\s*-->\s*(\d+:\d+:\d+,\d+)",
            lines[1]
        )

        if not match:
            continue

        def to_ms(timestamp):
            h, m, s = timestamp.split(":")
            s, ms = s.split(",")
            return (
                int(h) * 3600000
                + int(m) * 60000
                + int(s) * 1000
                + int(ms)
            )

        entries.append((
            to_ms(match.group(1)),
            to_ms(match.group(2)),
            "\n".join(lines[2:])
        ))

    return entries


def ass_time(ms):
    h, remainder = divmod(ms, 3600000)
    m, remainder = divmod(remainder, 60000)
    s, ms = divmod(remainder, 1000)

    return f"{h}:{m:02d}:{s:02d}.{ms // 10:02d}"


def escape_ass(text):
    return (
        text
        .replace("\\", r"\\")
        .replace("\n", r"\N")
    )


fr = parse_srt("fr.srt")
es = parse_srt("es.srt")

result = []
used_fr = set()


for es_index, (es_start, es_end, es_text) in enumerate(es):

    best_match = None
    best_overlap = 0

    for fr_index, (fr_start, fr_end, fr_text) in enumerate(fr):

        if fr_index in used_fr:
            continue

        overlap = min(es_end, fr_end) - max(es_start, fr_start)

        if overlap <= 0:
            continue

        if overlap > best_overlap:
            best_overlap = overlap
            best_match = (
                fr_index,
                fr_start,
                fr_end,
                fr_text
            )

    if best_match is not None:

        fr_index, fr_start, fr_end, fr_text = best_match
        used_fr.add(fr_index)

        text = (
            r"{\c&HFFFFFF&\fs38\i1}"
            + escape_ass(fr_text)
            + r"\N"
            + r"{\c&H00FFFF&\fs50\i0}"
            + escape_ass(es_text)
        )

        result.append((es_start, es_end, text))

    else:

        result.append((
            es_start,
            es_end,
            r"{\c&H00FFFF&\fs50\i0}" + escape_ass(es_text)
        ))


for fr_index, (fr_start, fr_end, fr_text) in enumerate(fr):

    if fr_index not in used_fr:

        result.append((
            fr_start,
            fr_end,
            r"{\c&HFFFFFF&\fs38\i1}" + escape_ass(fr_text)
        ))


result.sort(key=lambda x: (x[0], x[1]))


with open(output, "w", encoding="utf-8-sig") as f:

    f.write("""[Script Info]
Title: Spanish + French
ScriptType: v4.00+
PlayResX: 1920
PlayResY: 1080
ScaledBorderAndShadow: yes

[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
Style: Default,Arial,50,&H00FFFFFF,&H00FFFFFF,&H00000000,&H80000000,0,0,0,0,100,100,0,0,1,2,1,2,40,40,35,1

[Events]
Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
""")

    for start, end, text in result:

        f.write(
            f"Dialogue: 0,"
            f"{ass_time(start)},"
            f"{ass_time(end)},"
            f"Default,,0,0,0,,"
            f"{text}\n"
        )


Path("fr.srt").unlink()
Path("es.srt").unlink()

print(f"OK -> {output.name} ({len(result)} blocs)")
PY

    rm -f fr.srt es.srt
done
Améliorations futures

La V0 ne cherche pas à résoudre parfaitement tous les problèmes d'alignement.

L'algorithme pourra ensuite être amélioré pour gérer :

1 principal ↔ 1 secondaire
1 principal ↔ plusieurs secondaires
plusieurs principaux ↔ 1 secondaire
plusieurs principaux ↔ plusieurs secondaires

Des critères supplémentaires pourront être utilisés :

chevauchement temporel ;
distance temporelle ;
continuité des correspondances ;
durée des sous-titres ;
nombre de lignes ;
ponctuation ;
structure des dialogues ;
analyse textuelle ;
éventuellement comparaison sémantique.

Mais ne pas complexifier prématurément la V0.

Philosophie de SubStack

SubStack doit avant tout être une application GUI réellement accessible aux utilisateurs lambda.

L'utilisateur ne doit pas savoir :

ce qu'est FFmpeg ;
ce qu'est MKVToolNix ;
comment fonctionne un PATH ;
comment extraire une piste ;
comment créer un ASS ;
comment remuxer une vidéo.

Il doit simplement pouvoir :

Glisser → sélectionner → styliser → choisir la sortie → traiter.

La simplicité de l'expérience utilisateur est donc une exigence fondamentale du projet, et non un simple élément esthétique.


Architecture et qualité du code — EXIGENCE ABSOLUE

SubStack doit être développé avec une architecture propre, modulaire et maintenable.

Les principes SOLID doivent impérativement être respectés. Ils ne sont pas optionnels.

En particulier, aucune classe ne doit devenir un "God Object" regroupant l'interface graphique, la logique métier, FFmpeg, le parsing des sous-titres, la génération ASS et la gestion des fichiers.

Les responsabilités doivent être clairement séparées.

Par exemple, l'architecture pourra distinguer des composants/services tels que :

UI / JavaFX
    ↓
Application / Use Cases
    ↓
Domain / Business Logic
    ↓
Infrastructure
    ├── FFmpeg
    ├── Media containers
    ├── Subtitle parsing
    └── ASS generation

Les noms et la structure exacte restent à définir correctement lors de l'implémentation, mais la séparation des responsabilités est obligatoire.

Quelques principes à respecter :

Une classe = une responsabilité claire.
La logique métier ne doit pas dépendre directement de JavaFX.
La génération ASS ne doit pas connaître l'interface graphique.
Le parsing des sous-titres doit être indépendant de FFmpeg.
Le traitement vidéo/remux doit être isolé derrière une abstraction.
Les appels aux processus externes doivent être centralisés dans une couche d'infrastructure.
Les modèles métier doivent rester simples et indépendants de l'UI.
Éviter les dépendances inutiles entre composants.
Préférer l'injection de dépendances aux dépendances créées directement partout avec new.
Les composants doivent être facilement testables individuellement.
Taille des fichiers

Ne pas créer de fichiers de plusieurs milliers de lignes.

Un fichier de 3000 lignes contenant toute la logique de SubStack serait considéré comme une mauvaise architecture.

Si une classe commence à devenir importante, il faut se demander si plusieurs responsabilités ont été mélangées et extraire les composants concernés.

Il faut privilégier :

Petites classes
    ↓
Responsabilités claires
    ↓
Services composables
    ↓
Code testable

plutôt que :

MainController.java
    └── 3000+ lignes
        ├── UI
        ├── FFmpeg
        ├── parsing SRT
        ├── matching
        ├── ASS
        ├── fichiers
        └── configuration
Évolution du projet

L'architecture doit également permettre d'ajouter plus tard de nouveaux algorithmes de matching sans réécrire toute l'application.

Par exemple, la V0 peut utiliser un matcher basé sur le chevauchement temporel :

SubtitleMatcher
    └── TemporalOverlapMatcher (V0)

Puis éventuellement :

SubtitleMatcher
    ├── TemporalOverlapMatcher
    ├── AdvancedTemporalMatcher
    └── SemanticMatcher

L'UI ne devrait pas avoir besoin de connaître les détails de l'algorithme utilisé.

Même principe pour les formats et traitements multimédias : SubStack doit pouvoir évoluer sans transformer chaque modification en refactor massif.

Avant d'implémenter une fonctionnalité importante, réfléchir d'abord à la responsabilité du composant concerné et à son emplacement dans l'architecture. Ne pas simplement ajouter du code au fichier actuellement ouvert parce que c'est plus rapide.

La priorité est :

fonctionnalité correcte + UX simple + architecture propre + code maintenable.