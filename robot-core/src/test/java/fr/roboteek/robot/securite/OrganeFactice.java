package fr.roboteek.robot.securite;

/**
 * Organe surveillé réglable à la main, pour éprouver le registre et le watchdog sans
 * matériel : on décide de son état de service, de la date de son dernier battement et de ce qu'il
 * fait bouger.
 */
class OrganeFactice implements OrganeSurveille {

    private final String id;
    private final boolean provoqueUnMouvement;

    private boolean enService = true;
    private long dernierBattement = 0L;
    private boolean enMouvement = false;
    private NatureOrgane nature = NatureOrgane.ACTIONNEUR;

    OrganeFactice(String id, boolean provoqueUnMouvement) {
        this.id = id;
        this.provoqueUnMouvement = provoqueUnMouvement;
    }

    OrganeFactice enService(boolean enService) {
        this.enService = enService;
        return this;
    }

    OrganeFactice battuA(long instant) {
        this.dernierBattement = instant;
        return this;
    }

    OrganeFactice enMouvement(boolean enMouvement) {
        this.enMouvement = enMouvement;
        return this;
    }

    OrganeFactice nature(NatureOrgane nature) {
        this.nature = nature;
        return this;
    }

    @Override
    public NatureOrgane nature() {
        return nature;
    }

    @Override
    public String idOrgane() {
        return id;
    }

    @Override
    public String libelleOrgane() {
        return id;
    }

    @Override
    public boolean enService() {
        return enService;
    }

    @Override
    public long dernierBattement() {
        return dernierBattement;
    }

    @Override
    public boolean provoqueUnMouvement() {
        return provoqueUnMouvement;
    }

    @Override
    public boolean enMouvement() {
        return enMouvement;
    }
}
