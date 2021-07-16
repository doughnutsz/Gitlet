package gitlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;

import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Repository {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    /* TODO: fill in the rest of this class. */
    public static final File INDEX = join(GITLET_DIR, "index");

    public static final File OBJECTS_DIR = join(GITLET_DIR, "objects");

    private static final File HEAD = join(GITLET_DIR, "HEAD");

    private static final File REFS_DIR = join(GITLET_DIR, "refs");

    private static final File BRANCH_HEADS_DIR = join(REFS_DIR, "heads");

    private static final String DEFAULT_BRANCH_NAME = "master";

    private static final String HEAD_BRANCH_REF_PREFIX = "ref: refs/heads/";

    private static final File[] currentFiles = Objects.requireNonNull(CWD.listFiles(File::isFile));

    private final StagingArea stagingArea;

    private final String currentBranch;

    private final Commit HEADCommit;

    public Repository() {
        if (INDEX.exists()) {
            stagingArea = StagingArea.fromFile();
        } else {
            stagingArea = new StagingArea();
        }
        currentBranch = readContentsAsString(HEAD).replace(HEAD_BRANCH_REF_PREFIX, "");
        File branchHeadFile = join(BRANCH_HEADS_DIR, currentBranch);
        HEADCommit = Commit.fromFile(readContentsAsString(branchHeadFile));
        stagingArea.setTracked(HEADCommit.getTracked());
    }

    public static void init() {
        if (GITLET_DIR.exists()) {
            message("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }
        GITLET_DIR.mkdir();
        OBJECTS_DIR.mkdir();
        REFS_DIR.mkdir();
        BRANCH_HEADS_DIR.mkdir();
        writeContents(HEAD, HEAD_BRANCH_REF_PREFIX + DEFAULT_BRANCH_NAME);
        Commit initialCommit = new Commit();
        initialCommit.save();
        writeContents(join(BRANCH_HEADS_DIR, DEFAULT_BRANCH_NAME), initialCommit.getId());
    }

    public void add(String fileName) {
        File file;
        if (Paths.get(fileName).isAbsolute()) {
            file = new File(fileName);
        } else {
            file = join(CWD, fileName);
        }
        if (!file.exists()) {
            message("File does not exist.");
            System.exit(0);
        }
        if (stagingArea.addFile(file)) {
            stagingArea.save();
        }
    }

    public void commit(String message, String secondParent) {
        if (stagingArea.isClean()) {
            message("No changes added to the commit.");
            System.exit(0);
        }
        Map<String, String> newTrackedFilesMap = stagingArea.commit();
        stagingArea.save();
        List<String> parents = new ArrayList<>();
        parents.add(HEADCommit.getId());
        if (secondParent != null) {
            parents.add(secondParent);
        }
        Commit newCommit = new Commit(message, new String[]{HEADCommit.getId()}, newTrackedFilesMap);
        newCommit.save();
        writeContents(join(BRANCH_HEADS_DIR, currentBranch), newCommit.getId());
    }

    public void remove(String fileName) {
        File file;
        if (Paths.get(fileName).isAbsolute()) {
            file = new File(fileName);
        } else {
            file = join(CWD, fileName);
        }
        if (stagingArea.removeFile(file)) {
            stagingArea.save();
        } else {
            message("No reason to remove the file.");
            System.exit(0);
        }
    }

    public void log() {
        StringBuilder logBuilder = new StringBuilder();
        Commit currentCommit = HEADCommit;
        while (true) {
            logBuilder.append(currentCommit.getLog()).append("\n");
            String[] parentCommitIds = currentCommit.getParents();
            if (parentCommitIds.length == 0) {
                break;
            }
            String firstParentCommitId = parentCommitIds[0];
            currentCommit = Commit.fromFile(firstParentCommitId);
        }
        System.out.print(logBuilder);
    }

    private static void forEachCommitInOrder(Consumer<Commit> cb) {
        Queue<Commit> commitsQueue = new PriorityQueue<>((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        Set<String> checkedCommitIds = new HashSet<>();

        File[] branchHeadFiles = BRANCH_HEADS_DIR.listFiles();
        Arrays.sort(branchHeadFiles, Comparator.comparing(File::getName));

        for (File branchHeadFile : branchHeadFiles) {
            String branchHeadCommitId = readContentsAsString(branchHeadFile);
            if (checkedCommitIds.contains(branchHeadCommitId)) {
                continue;
            }
            checkedCommitIds.add(branchHeadCommitId);
            Commit branchHeadCommit = Commit.fromFile(branchHeadCommitId);
            commitsQueue.add(branchHeadCommit);
        }

        while (true) {
            Commit latestCommit = commitsQueue.poll();
            cb.accept(latestCommit);
            String[] parentCommitIds = latestCommit.getParents();
            if (parentCommitIds.length == 0) {
                break;
            }
            for (String parentCommitId : parentCommitIds) {
                if (checkedCommitIds.contains(parentCommitId)) {
                    continue;
                }
                checkedCommitIds.add(parentCommitId);
                Commit parentCommit = Commit.fromFile(parentCommitId);
                commitsQueue.add(parentCommit);
            }
        }
    }

    public static void globalLog() {
        StringBuilder logBuilder = new StringBuilder();
        forEachCommitInOrder(commit -> logBuilder.append(commit.getLog()).append("\n"));
        System.out.print(logBuilder);
    }

    public static void find(String message) {
        StringBuilder resultBuilder = new StringBuilder();
        forEachCommitInOrder(commit -> {
            if (commit.getMessage().equals(message)) {
                resultBuilder.append(commit.getId()).append("\n");
            }
        });
        if (resultBuilder.length() == 0) {
            message("Found no commit with that message.");
            System.exit(0);
        }
        System.out.print(resultBuilder);
    }

    private static void appendFileNamesInOrder(StringBuilder stringBuilder, Collection<String> filePathsCollection) {
        List<String> filePathsList = new ArrayList<>(filePathsCollection);
        appendFileNamesInOrder(stringBuilder, filePathsList);
    }

    private static void appendFileNamesInOrder(StringBuilder stringBuilder, List<String> filePathsList) {
        filePathsList.sort(String::compareTo);
        for (String filePath : filePathsList) {
            String fileName = Paths.get(filePath).getFileName().toString();
            stringBuilder.append(fileName).append("\n");
        }
    }

    private static Map<String, String> getCurrentFilesMap() {
        Map<String, String> filesMap = new HashMap<>();
        for (File file : currentFiles) {
            String filePath = file.getPath();
            String blobId = Blob.generateId(file);
            filesMap.put(filePath, blobId);
        }
        return filesMap;
    }

    public void status() {
        StringBuilder statusBuilder = new StringBuilder();
        statusBuilder.append("=== Branches ===").append("\n");
        statusBuilder.append("*").append(currentBranch).append("\n");
        String[] branchNames = BRANCH_HEADS_DIR.list((dir, name) -> !name.equals(currentBranch));
        Arrays.sort(branchNames);
        for (String branchName : branchNames) {
            statusBuilder.append(branchName).append("\n");
        }
        statusBuilder.append("\n");
        Map<String, String> addedFilesMap = stagingArea.getAdded();
        Set<String> removedFilePathsSet = stagingArea.getRemoved();
        statusBuilder.append("=== Staged Files ===").append("\n");
        appendFileNamesInOrder(statusBuilder, addedFilesMap.keySet());
        statusBuilder.append("\n");
        statusBuilder.append("=== Removed Files ===").append("\n");
        appendFileNamesInOrder(statusBuilder, removedFilePathsSet);
        statusBuilder.append("\n");
        statusBuilder.append("=== Modifications Not Staged For Commit ===").append("\n");
        List<String> modifiedNotStageFilePaths = new ArrayList<>();
        Set<String> deletedNotStageFilePaths = new HashSet<>();
        Map<String, String> currentFilesMap = getCurrentFilesMap();
        Map<String, String> trackedFilesMap = HEADCommit.getTracked();
        trackedFilesMap.putAll(addedFilesMap);
        for (String filePath : removedFilePathsSet) {
            trackedFilesMap.remove(filePath);
        }
        for (Map.Entry<String, String> entry : trackedFilesMap.entrySet()) {
            String filePath = entry.getKey();
            String blobId = entry.getValue();
            String currentFileBlobId = currentFilesMap.get(filePath);
            if (currentFileBlobId != null) {
                if (!currentFileBlobId.equals(blobId)) {
                    modifiedNotStageFilePaths.add(filePath);
                }
                currentFilesMap.remove(filePath);
            } else {
                modifiedNotStageFilePaths.add(filePath);
                deletedNotStageFilePaths.add(filePath);
            }
        }
        modifiedNotStageFilePaths.sort(String::compareTo);
        for (String filePath : modifiedNotStageFilePaths) {
            String fileName = Paths.get(filePath).getFileName().toString();
            statusBuilder.append(fileName);
            if (deletedNotStageFilePaths.contains(filePath)) {
                statusBuilder.append(" (deleted)");
            } else {
                statusBuilder.append(" (modified)");
            }
            statusBuilder.append("\n");
        }
        statusBuilder.append("\n");
        statusBuilder.append("=== Untracked Files ===").append("\n");
        appendFileNamesInOrder(statusBuilder, currentFilesMap.keySet());
        statusBuilder.append("\n");
        System.out.print(statusBuilder);
    }

    public static boolean isFileInstanceOf(File file, Class<?> c) {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            return c.isInstance(in.readObject());
        } catch (Exception e) {
            return false;
        }
    }

    private static String getActualCommitId(String commitId) {
        if (commitId.length() < UID_LENGTH) {
            if (commitId.length() < 4) {
                message("Commit id should contain at least 4 characters.");
                System.exit(0);
            }
            String objectDirName = commitId.substring(0, 2);
            File objectDir = join(OBJECTS_DIR, objectDirName);
            if (!objectDir.exists()) {
                message("No commit with that id exists.");
                System.exit(0);
            }
            String objectFileNamePrefix = commitId.substring(2);
            boolean isFound = false;
            File[] objectFiles = objectDir.listFiles();
            for (File objectFile : objectFiles) {
                String objectFileName = objectFile.getName();
                if (objectFileName.startsWith(objectFileNamePrefix) && isFileInstanceOf(objectFile, Commit.class)) {
                    if (isFound) {
                        message("More than 1 commit has the same id prefix.");
                        System.exit(0);
                    }
                    commitId = objectDirName + objectFileName;
                    isFound = true;
                }
            }
            if (!isFound) {
                message("No commit with that id exists.");
                System.exit(0);
            }
        } else {
            if (!join(Repository.OBJECTS_DIR, commitId.substring(0, 2), commitId.substring(2)).exists()) {
                message("No commit with that id exists.");
                System.exit(0);
            }
        }
        return commitId;
    }

    public void checkout(String commitId, String fileName) {
        commitId = getActualCommitId(commitId);
        File file;
        if (Paths.get(fileName).isAbsolute()) {
            file = new File(fileName);
        } else {
            file = join(CWD, fileName);
        }
        String filePath = file.getPath();
        if (!Commit.fromFile(commitId).restoreTracked(filePath)) {
            message("File does not exist in that commit.");
            System.exit(0);
        }
    }

    public void checkout(String fileName) {
        File file;
        if (Paths.get(fileName).isAbsolute()) {
            file = new File(fileName);
        } else {
            file = join(CWD, fileName);
        }
        String filePath = file.getPath();
        if (!HEADCommit.restoreTracked(filePath)) {
            message("File does not exist in that commit.");
            System.exit(0);
        }
    }

    private void checkUntracked(Commit targetCommit) {
        Map<String, String> currentFilesMap = getCurrentFilesMap();
        Map<String, String> trackedFilesMap = HEADCommit.getTracked();
        Map<String, String> addedFilesMap = stagingArea.getAdded();
        Set<String> removedFilePathsSet = stagingArea.getRemoved();

        List<String> untrackedFilePaths = new ArrayList<>();

        for (String filePath : currentFilesMap.keySet()) {
            if (trackedFilesMap.containsKey(filePath)) {
                if (removedFilePathsSet.contains(filePath)) {
                    untrackedFilePaths.add(filePath);
                }
            } else {
                if (!addedFilesMap.containsKey(filePath)) {
                    untrackedFilePaths.add(filePath);
                }
            }
        }

        Map<String, String> targetCommitTrackedFilesMap = targetCommit.getTracked();

        for (String filePath : untrackedFilePaths) {
            String blobId = currentFilesMap.get(filePath);
            String targetBlobId = targetCommitTrackedFilesMap.get(filePath);
            if (!blobId.equals(targetBlobId)) {
                message("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
        }
    }

    public void checkoutBranch(String branchName) {
        File branchHeadFile = join(BRANCH_HEADS_DIR, branchName);
        if (!branchHeadFile.exists()) {
            message("No such branch exists.");
            System.exit(0);
        }
        if (branchName.equals(currentBranch)) {
            message("No need to checkout the current branch.");
            System.exit(0);
        }
        Commit branchHeadCommit = Commit.fromFile(readContentsAsString(branchHeadFile));
        checkUntracked(branchHeadCommit);
        stagingArea.clear();
        stagingArea.save();
        for (File file : currentFiles) {
            file.delete();
        }
        branchHeadCommit.restoreAllTracked();
        writeContents(HEAD, HEAD_BRANCH_REF_PREFIX + branchName);
    }

    public void branch(String branchName) {
        File branchHeadFile = join(BRANCH_HEADS_DIR, branchName);
        if (branchHeadFile.exists()) {
            message("A branch with that name already exists.");
            System.exit(0);
        }
        writeContents(branchHeadFile, HEADCommit.getId());
    }

    public void rmBranch(String branchName) {
        File branchHeadFile = join(BRANCH_HEADS_DIR, branchName);
        if (!branchHeadFile.exists()) {
            message("A branch with that name does not exist.");
            System.exit(0);
        }
        if (branchName.equals(currentBranch)) {
            message("Cannot remove the current branch.");
            System.exit(0);
        }
        branchHeadFile.delete();
    }

    public void reset(String commitId) {
        commitId = getActualCommitId(commitId);
        Commit targetCommit = Commit.fromFile(commitId);
        checkUntracked(targetCommit);
        stagingArea.clear();
        stagingArea.save();
        for (File file : currentFiles) {
            file.delete();
        }
        targetCommit.restoreAllTracked();
        writeContents(join(BRANCH_HEADS_DIR, currentBranch), commitId);
    }

    private static String getConflictContent(String currentBlobId, String targetBlobId) {
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append("<<<<<<< HEAD").append("\n");
        if (currentBlobId != null) {
            Blob currentBlob = Blob.fromFile(currentBlobId);
            contentBuilder.append(new String(currentBlob.getContent(), StandardCharsets.UTF_8));
        }
        contentBuilder.append("=======").append("\n");
        if (targetBlobId != null) {
            Blob targetBlob = Blob.fromFile(targetBlobId);
            contentBuilder.append(new String(targetBlob.getContent(), StandardCharsets.UTF_8));
        }
        contentBuilder.append(">>>>>>>");
        return contentBuilder.toString();
    }

    private static Commit getLatestCommonAncestorCommit(Commit commitA, Commit commitB) {
        Comparator<Commit> commitComparator = Comparator.comparing(Commit::getTimestamp).reversed();
        Queue<Commit> commitsQueue = new PriorityQueue<>(commitComparator);
        commitsQueue.add(commitA);
        commitsQueue.add(commitB);
        Set<String> checkedCommitIds = new HashSet<>();
        while (true) {
            Commit latestCommit = commitsQueue.poll();
            List<String> parentCommitIds = Arrays.asList(latestCommit.getParents());
            for (String parentCommitId : parentCommitIds) {
                Commit parentCommit = Commit.fromFile(parentCommitId);
                if (checkedCommitIds.contains(parentCommitId)) {
                    return parentCommit;
                }
                commitsQueue.add(parentCommit);
                checkedCommitIds.add(parentCommitId);
            }
        }
    }

    public void merge(String targetBranchName) {
        File targetBranchHeadFile = join(BRANCH_HEADS_DIR, targetBranchName);
        if (!targetBranchHeadFile.exists()) {
            message("A branch with that name does not exist.");
            System.exit(0);
        }
        if (targetBranchName.equals(currentBranch)) {
            message("Cannot merge a branch with itself.");
            System.exit(0);
        }
        if (!stagingArea.isClean()) {
            message("You have uncommitted changes.");
            System.exit(0);
        }
        Commit targetBranchHeadCommit = Commit.fromFile(readContentsAsString(targetBranchHeadFile));
        checkUntracked(targetBranchHeadCommit);
        Commit lcaCommit = getLatestCommonAncestorCommit(HEADCommit, targetBranchHeadCommit);
        String lcaCommitId = lcaCommit.getId();
        if (lcaCommitId.equals(targetBranchHeadCommit.getId())) {
            message("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        if (lcaCommitId.equals(HEADCommit.getId())) {
            stagingArea.clear();
            stagingArea.save();
            for (File file : currentFiles) {
                file.delete();
            }
            targetBranchHeadCommit.restoreAllTracked();
            writeContents(HEAD, HEAD_BRANCH_REF_PREFIX + targetBranchName);
            message("Current branch fast-forwarded.");
            System.exit(0);
        }
        boolean hasConflict = false;
        Map<String, String> HEADCommitTrackedFilesMap = new HashMap<>(HEADCommit.getTracked());
        Map<String, String> targetBranchHeadCommitTrackedFilesMap = targetBranchHeadCommit.getTracked();
        Map<String, String> lcaCommitTrackedFilesMap = lcaCommit.getTracked();
        for (Map.Entry<String, String> entry : lcaCommitTrackedFilesMap.entrySet()) {
            String filePath = entry.getKey();
            File file = new File(filePath);
            String blobId = entry.getValue();
            String targetBranchHeadCommitBlobId = targetBranchHeadCommitTrackedFilesMap.get(filePath);
            String HEADCommitBlobId = HEADCommitTrackedFilesMap.get(filePath);
            if (targetBranchHeadCommitBlobId != null) { // exists in the target branch
                if (!targetBranchHeadCommitBlobId.equals(blobId)) { // modified in the target branch
                    if (HEADCommitBlobId != null) { // exists in the current branch
                        if (HEADCommitBlobId.equals(blobId)) { // not modified in the current branch
                            // case 1
                            Blob.fromFile(targetBranchHeadCommitBlobId).writeContentToSource();
                            stagingArea.addFile(file);
                        } else { // modified in the current branch
                            if (!HEADCommitBlobId.equals(targetBranchHeadCommitBlobId)) { // modified in different ways
                                // case 8
                                hasConflict = true;
                                String conflictContent = getConflictContent(HEADCommitBlobId, targetBranchHeadCommitBlobId);
                                writeContents(file, conflictContent);
                                stagingArea.addFile(file);
                            } // else modified in the same ways
                            // case 3
                        }
                    } else { // deleted in current branch
                        // case 8
                        hasConflict = true;
                        String conflictContent = getConflictContent(null, targetBranchHeadCommitBlobId);
                        writeContents(file, conflictContent);
                        stagingArea.addFile(file);
                    }
                } // else not modified in the target branch
                // case 2, case 7
            } else { // deleted in the target branch
                if (HEADCommitBlobId != null) { // exists in the current branch
                    if (HEADCommitBlobId.equals(blobId)) { // not modified in the current branch
                        // case 6
                        stagingArea.removeFile(file);
                    } else { // modified in the current branch
                        // case 8
                        hasConflict = true;
                        String conflictContent = getConflictContent(HEADCommitBlobId, null);
                        writeContents(file, conflictContent);
                        stagingArea.addFile(file);
                    }
                } // else deleted in both branches
                // case 3
            }
            HEADCommitTrackedFilesMap.remove(filePath);
            targetBranchHeadCommitTrackedFilesMap.remove(filePath);
        }
        for (Map.Entry<String, String> entry : targetBranchHeadCommitTrackedFilesMap.entrySet()) {
            String targetBranchHeadCommitFilePath = entry.getKey();
            File targetBranchHeadCommitFile = new File(targetBranchHeadCommitFilePath);
            String targetBranchHeadCommitBlobId = entry.getValue();
            String HEADCommitBlobId = HEADCommitTrackedFilesMap.get(targetBranchHeadCommitFilePath);
            if (HEADCommitBlobId != null) { // added in both branches
                if (!HEADCommitBlobId.equals(targetBranchHeadCommitBlobId)) { // modified in different ways
                    // case 8
                    hasConflict = true;
                    String conflictContent = getConflictContent(HEADCommitBlobId, targetBranchHeadCommitBlobId);
                    writeContents(targetBranchHeadCommitFile, conflictContent);
                    stagingArea.addFile(targetBranchHeadCommitFile);
                } // else modified in the same ways
                // case 3
            } else { // only added in the target branch
                // case 5
                Blob.fromFile(targetBranchHeadCommitBlobId).writeContentToSource();
                stagingArea.addFile(targetBranchHeadCommitFile);
            }
        }
        String newCommitMessage = "Merged" + " " + targetBranchName + " " + "into" + " " + currentBranch + ".";
        commit(newCommitMessage, targetBranchHeadCommit.getId());
        if (hasConflict) {
            message("Encountered a merge conflict.");
        }
    }
}
