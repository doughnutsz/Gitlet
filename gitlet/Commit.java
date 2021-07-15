package gitlet;

// TODO: any imports you need here

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date; // TODO: You'll likely use this in this class
import java.util.HashMap;
import java.util.Map;
import static gitlet.Utils.*;
import static gitlet.Repository.*;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Commit implements Serializable {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private final String message;

    /* TODO: fill in the rest of this class. */
    private final Map<String, String> tracked;

    private final String timestamp;

    private final String[] parents;

    private final String id;

    private final File file;

    public Commit(String message, String[] parents, Map<String, String> tracked){
        this.message = message;
        this.parents = parents;
        this.tracked = tracked;
        Date d = new Date(System.currentTimeMillis());
        timestamp = String.format("Date: %tc",d);
        id = sha1(timestamp, message, Arrays.toString(parents), tracked.toString());
        file = join(Repository.OBJECTS_DIR, id.substring(0,2),id.substring(2));
    }

    public Commit() {
        timestamp = String.format("Date: %tc",new Date(0));
        message = "initial commit";
        parents = new String[0];
        tracked = new HashMap<>();
        id = sha1(timestamp, message, Arrays.toString(parents), tracked.toString());
        file = join(Repository.OBJECTS_DIR, id.substring(0,2),id.substring(2));
    }

    public static Commit fromFile(String id) {
        // TODO (hint: look at the Utils file)
        File a = join(OBJECTS_DIR,id.substring(0,2),id.substring(2));
        Commit c = readObject(a, Commit.class);
        return c;
    }

    public void saveCommit() {
        // TODO (hint: don't forget dog names are unique)
        File a = join(OBJECTS_DIR,id.substring(0,2),id.substring(2));
        try {
            a.createNewFile();
        } catch (IOException e) {
        }
        writeObject(a,this);
    }

    public boolean restoreTracked(String filePath) {
        String blobId = tracked.get(filePath);
        if (blobId == null) {
            return false;
        }
        Blob.fromFile(blobId).writeContentToSource();
        return true;
    }

    public void restoreAllTracked() {
        for (String blobId : tracked.values()) {
            Blob.fromFile(blobId).writeContentToSource();
        }
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getTracked() {
        return tracked;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String[] getParents() {
        return parents;
    }

    public String getId() {
        return id;
    }

    public String getLog() {
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("===").append("\n");
        logBuilder.append("commit").append(" ").append(id).append("\n");
        if (parents.length > 1) {
            logBuilder.append("Merge:");
            for (String parent : parents) {
                logBuilder.append(" ").append(parent, 0, 7);
            }
            logBuilder.append("\n");
        }
        logBuilder.append(getTimestamp()).append("\n");
        logBuilder.append(message).append("\n");
        return logBuilder.toString();
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
