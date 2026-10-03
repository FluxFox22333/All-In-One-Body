package dev.fivefold.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TradeLayoutTest {
 @Test void offerRowsRespectNativeCoordinatesAndScroll(){assertEquals(0,TradeLayout.offerAt(5,18,0,12));assertEquals(6,TradeLayout.offerAt(92,157,0,12));assertEquals(10,TradeLayout.offerAt(20,118,5,12));}
 @Test void outsideAndUnlistedOffersCannotSelect(){assertEquals(-1,TradeLayout.offerAt(136,37,0,12));assertEquals(-1,TradeLayout.offerAt(10,158,0,12));assertEquals(-1,TradeLayout.offerAt(10,98,0,4));}
 @Test void scrollbarClampsToAvailablePages(){assertEquals(0,TradeLayout.scrollAt(18,15));assertEquals(8,TradeLayout.scrollAt(157,15));assertEquals(0,TradeLayout.scrollAt(157,4));}
}
