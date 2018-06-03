AI1=GMCTSL
AI2=MCTSL
AI1S=${1:-8}
AI2S=${2:-8}
POOL=${3-Magarena/decks/aisandbox/}
java -cp Magarena.jar magic.AiStrCal \
  --ai1 $AI1 \
  --str1 $AI1S \
  --ai2 $AI2 \
  --str2 $AI2S \
  --deckpool $POOL\
  --games 2
