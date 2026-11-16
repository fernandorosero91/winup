package com.winup.model.match;
import com.winup.model.vo.Dinero;


public record SettlementLine(Bet bet, boolean won, Dinero payout) {
}
