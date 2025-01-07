class ScoreEntry {
    private final int id;
    private int score;
    private final int country;  // New field for country information

    public ScoreEntry(int id, int score, int country) {
        this.id = id;
        this.score = score;
        this.country = country;
    }

    public int getId() {
        return id;
    }

    public int getScore() {
        return score;
    }

    public int getCountry() {
        return country;
    }

    public void addScore(int points) {
        this.score += points;
    }

    @Override
    public String toString() {
        return String.format("(ID: %d, Country: %d, Score: %d)", id, country, score);
    }
}