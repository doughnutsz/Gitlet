package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static gitlet.Utils.readObject;
import static gitlet.Utils.writeObject;

public class StagingArea implements Serializable {
    private transient Map<String, String> tracked;

    private final Map<String, String> added = new HashMap<>();

    private final Set<String> removed = new HashSet<>();

    public Map<String, String> getTracked() {
        return tracked;
    }

    public Map<String, String> getAdded() {
        return added;
    }

    public Set<String> getRemoved() {
        return removed;
    }

    public void setTracked(Map<String, String> filesMap) {
        tracked = filesMap;
    }

    public boolean isClean() {
        return added.isEmpty() && removed.isEmpty();
    }

    public Map<String, String> commit() {
        tracked.putAll(added);
        for (String filePath : removed) {
            tracked.remove(filePath);
        }
        clear();
        return tracked;
    }

    public static StagingArea fromFile() {
        return readObject(Repository.INDEX, StagingArea.class);
    }

    public void save() {
        writeObject(Repository.INDEX, this);
    }

    public boolean addFile(File file) {
        String filePath = file.getPath();
        Blob blob = new Blob(file);
        String blobId = blob.getId();
        String trackedBlobId = tracked.get(filePath);
        if (trackedBlobId != null) {
            if (trackedBlobId.equals(blobId)) {
                if (added.remove(filePath) != null) {
                    return true;
                }
                return removed.remove(filePath);
            }
        }
        String prevBlobId = added.put(filePath, blobId);
        if (prevBlobId != null && prevBlobId.equals(blobId)) {
            return false;
        }
        if (!blob.getFile().exists()) {
            blob.save();
        }
        return true;
    }

    public boolean removeFile(File file) {
        String filePath = file.getPath();
        String addedBlobId = added.remove(filePath);
        if (addedBlobId != null) {
            return true;
        }
        if (tracked.get(filePath) != null) {
            if (file.exists()) {
                if (!file.delete()) {
                    throw new IllegalArgumentException(String.format("rm: %s: Failed to delete.", file.getPath()));
                }
            }
            return removed.add(filePath);
        }
        return false;
    }

    public void clear() {
        added.clear();
        removed.clear();
    }
}
