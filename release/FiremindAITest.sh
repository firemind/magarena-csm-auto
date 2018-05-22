#java -ea -cp Magarena.jar magic.DeckStrCal \
AI1=GMCTS
AI2=MCTS
AI1S=${1:-4}
AI2S=${2:-4}
DECK1=${3:-Vanilla}
DECK2=${4:-Vanilla}
java -cp Magarena.jar magic.DeckStrCal \
  --deck1 Magarena/decks/prebuilt/$DECK1.dec \
  --ai1 $AI1 \
  --str1 $AI1S \
  --deck2 Magarena/decks/prebuilt/$DECK2.dec \
  --ai2 $AI2 \
  --str2 $AI2S \
  --games 10000 >> aitestresults/$AI1-$AI1S-$AI2-$AI2S.log

#  --deck2 Magarena/decks/prebuilt/Black_and_White.dec \
