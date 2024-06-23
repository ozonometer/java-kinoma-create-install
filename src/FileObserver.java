import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

class FileObserver implements Observer {
    private BufferedWriter writer;

    public FileObserver(String fileName) throws IOException {
        writer = new BufferedWriter(new FileWriter(fileName));
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof VariableHolder) {
            VariableHolder holder = (VariableHolder) o;
            try {
                writer.write("ip=" + holder.getIp() + "\n");
                writer.write("appName=" + holder.getAppName() + "\n");
                writer.write("path=" + holder.getPath() + "\n");
                writer.flush(); // write data immediately
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void close() throws IOException {
        writer.close();
    }
}