package gitlet;

import static gitlet.Utils.message;
import static gitlet.Repository.*;
/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        // TODO: what if args is empty?
        if (args.length == 0) {
            message("Please enter a command.");
            System.exit(0);
        }

        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                // TODO: handle the `init` command
                validateNumArgs(args, 1);
                Repository.init();
                break;
            case "add":
                // TODO: handle the `add [filename]` command
                if (!(GITLET_DIR.exists() && GITLET_DIR.isDirectory())) {
                    message("Not in an initialized Gitlet directory.");
                    System.exit(0);
                }
                validateNumArgs(args, 2);
                new Repository().add(args[1]);
                break;
            // TODO: FILL THE REST IN
            case "commit":
                if (!(GITLET_DIR.exists() && GITLET_DIR.isDirectory())) {
                    message("Not in an initialized Gitlet directory.");
                    System.exit(0);
                }
                validateNumArgs(args, 2);
                String message = args[1];
                if (message.length() == 0) {
                    message("Please enter a commit message.");
                    System.exit(0);
                }
                new Repository().commit(message);
                break;
            case "rm":
                if (!(GITLET_DIR.exists() && GITLET_DIR.isDirectory())) {
                    message("Not in an initialized Gitlet directory.");
                    System.exit(0);
                }
                validateNumArgs(args, 2);
                new Repository().remove(args[1]);
                break;
            case "log":
                if (!(GITLET_DIR.exists() && GITLET_DIR.isDirectory())) {
                    message("Not in an initialized Gitlet directory.");
                    System.exit(0);
                }
                validateNumArgs(args, 1);
                new Repository().log();
                break;
            case "global-log":
                if (!(GITLET_DIR.exists() && GITLET_DIR.isDirectory())) {
                    message("Not in an initialized Gitlet directory.");
                    System.exit(0);
                }
                validateNumArgs(args, 1);
                Repository.globalLog();
                break;
            case "find":
                if (!(GITLET_DIR.exists() && GITLET_DIR.isDirectory())) {
                    message("Not in an initialized Gitlet directory.");
                    System.exit(0);
                }
                validateNumArgs(args, 2);
                String findmessage = args[1];
                if (findmessage.length() == 0) {
                    message("Found no commit with that message.");
                    System.exit(0);
                }
                Repository.find(findmessage);
                break;
            case "status":
                if (!(GITLET_DIR.exists() && GITLET_DIR.isDirectory())) {
                    message("Not in an initialized Gitlet directory.");
                    System.exit(0);
                }
                validateNumArgs(args, 1);
                new Repository().status();
                break;
            case "checkout":
                if (!(GITLET_DIR.exists() && GITLET_DIR.isDirectory())) {
                    message("Not in an initialized Gitlet directory.");
                    System.exit(0);
                }
                Repository repository = new Repository();
                switch (args.length) {
                    case 3 :
                        if (!args[1].equals("--")) {
                            message("Incorrect operands.");
                            System.exit(0);
                        }
                        String fileName = args[2];
                        repository.checkout(fileName);
                        break;
                    case 4 :
                        if (!args[2].equals("--")) {
                            message("Incorrect operands.");
                            System.exit(0);
                        }
                        String commitId = args[1];
                        String fileNa = args[3];
                        repository.checkout(commitId, fileNa);
                        break;
                    case 2 :
                        String branch = args[1];
                        repository.checkoutBranch(branch);
                        break;
                    default :
                        message("Incorrect operands.");
                        System.exit(0);
                        break;
                }
                break;
            case "branch":
                if (!(GITLET_DIR.exists() && GITLET_DIR.isDirectory())) {
                    message("Not in an initialized Gitlet directory.");
                    System.exit(0);
                }
                validateNumArgs(args, 2);
                String branchName = args[1];
                new Repository().branch(branchName);
                break;
            case "rm-branch":
                if (!(GITLET_DIR.exists() && GITLET_DIR.isDirectory())) {
                    message("Not in an initialized Gitlet directory.");
                    System.exit(0);
                }
                validateNumArgs(args, 2);
                String branchNa = args[1];
                new Repository().rmBranch(branchNa);
                break;
            case "reset":
                if (!(GITLET_DIR.exists() && GITLET_DIR.isDirectory())) {
                    message("Not in an initialized Gitlet directory.");
                    System.exit(0);
                }
                validateNumArgs(args, 2);
                String commitId = args[1];
                new Repository().reset(commitId);
                break;
            default:
                message("No command with that name exists.");
                System.exit(0);
        }
    }

    private static void validateNumArgs(String[] args, int n) {
        if (args.length != n) {
            message("Incorrect operands.");
            System.exit(0);
        }
    }
}
