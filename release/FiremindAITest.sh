#java -ea -cp Magarena.jar magic.DeckStrCal \
java -cp Magarena.jar magic.DeckStrCal \
  --deck1 Magarena/decks/prebuilt/Vanilla.dec \
  --ai1 GMCTS \
  --str1 4 \
  --deck2 Magarena/decks/prebuilt/Vanilla.dec \
  --ai2 MCTS \
  --str2 4 \
  --games 100
