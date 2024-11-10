import java.io.Serializable;

public class Card implements Serializable {
    String suit, rank;
    private int value;

    public Card(String r, String s) {
        suit = s;
        rank = r;
    }

    public void setValue(int val) {
        value = val;
    }

    public int getValue() {
        return value;
    }

    public String getRank() {
        return rank;
    }

    public String ToString() {
        return rank + " of " + suit;
    }
}
