package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import static gitlet.Repository.GITLET_DIR;
import static gitlet.Repository.OBJECTS_DIR;
import static gitlet.Utils.*;
import static gitlet.Utils.writeObject;

public class Blob implements Serializable {
    private final String id;

    private final byte[] content;

    private final File source;

    private final File file;

    public Blob(File source) {
        this.source = source;
        content = readContents(source);
        id = sha1(content);
        file = join(Repository.OBJECTS_DIR, id.substring(0,2),id.substring(2));
    }

    public static String generateId(File source) {
        byte[] fileContent = readContents(source);
        return sha1(fileContent);
    }

    public static Blob fromFile(String id) {
        // TODO (hint: look at the Utils file)
        File a = join(OBJECTS_DIR,id.substring(0,2),id.substring(2));
        Blob b = readObject(a, Blob.class);
        return b;
    }

    public void saveBlob() {
        // TODO (hint: don't forget dog names are unique)
        File a = join(OBJECTS_DIR,id.substring(0,2),id.substring(2));
        try {
            a.createNewFile();
        } catch (IOException e) {
        }
        writeObject(a,this);
    }

    public void writeContentToSource() {
        writeContents(source, content);
    }

    public String getId() {
        return id;
    }

    public byte[] getContent() {
        return content;
    }

    public File getSource() {
        return source;
    }

    public File getFile() {
        return file;
    }

    public void save() {
        File dir = file.getParentFile();
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                throw new IllegalArgumentException(String.format("mkdir: %s: Failed to create.", dir.getPath()));
            }
        }
        writeObject(file, this);
    }
}
