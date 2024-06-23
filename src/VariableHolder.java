import java.util.Observable;

class VariableHolder extends Observable {
    private String ip;
    private String appName;
    private String path;
    public VariableHolder(String ip, String appName, String path) {
        this.ip = ip;
        this.appName = appName;
        this.path = path;
    }
    public void updateVariables(String ip, String appName, String path) {
        this.ip = ip;
        this.appName = appName;
        this.path = path;
        setChanged();
        notifyObservers();
    }
    public String getIp() {
        return ip;
    }

    public String getAppName() {
        return appName;
    }

    public String getPath() {
        return path;
    }
}