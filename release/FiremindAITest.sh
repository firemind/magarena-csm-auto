#java -ea -cp Magarena.jar magic.DeckStrCal \
AI1=GMCTS
AI2=MCTS
AI1S=${1:-1}
AI2S=${2:-1}
DECK1=${3-VanillaGreen}
DECK2=${4-VanillaWhite}
if [ ! -z $DECK1 ]; then
  DECK1_PARAM="--deck1 Magarena/decks/prebuilt/$DECK1.dec"
fi
if [ ! -z $DECK2 ]; then
  DECK2_PARAM="--deck2 Magarena/decks/prebuilt/$DECK2.dec"
fi
java -cp Magarena.jar magic.DeckStrCal \
  --ai1 $AI1 \
  --str1 $AI1S \
  --ai2 $AI2 \
  --str2 $AI2S \
  $DECK1_PARAM \
  $DECK2_PARAM \
  --games 10000 >> aitestresults/$AI1-$AI1S-$AI2-$AI2S.log

#  --deck2 Magarena/decks/prebuilt/Black_and_White.dec \
