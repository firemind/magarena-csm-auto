#java -ea -cp Magarena.jar magic.DeckStrCal \
AI1=GMCTS
AI2=MCTS
AI1S=${1:-4}
AI2S=${2:-4}
POOL=${3-Magarena/decks/aisandbox/}
java -cp Magarena.jar magic.AiStrCal \
  --ai1 $AI1 \
  --str1 $AI1S \
  --ai2 $AI2 \
  --str2 $AI2S \
  --deckpool $POOL\
  --games 2 | tee -a aitestresults/$AI1-$AI1S-$AI2-$AI2S.log

#  --deck2 Magarena/decks/prebuilt/Black_and_White.dec \
