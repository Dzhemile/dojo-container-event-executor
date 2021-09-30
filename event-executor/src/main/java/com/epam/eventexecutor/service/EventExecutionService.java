package com.epam.eventexecutor.service;

import com.epam.eventexecutor.model.ParticipantPushDetails;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

@Service
public class EventExecutionService {

    private static final String APP_WORKSPACE = "/app/";
    private static final int DEFAULT_FAIL_EXIT_CODE = -1;
    private static final int DEFAULT_SUCCESS_EXIT_CODE = 0;
    private final ExecutorService participantEventThreadPool;
    private final Map<String, Boolean> userPushEventTaskCache;

    @Autowired
    public EventExecutionService(@Qualifier("participantEventThreadExecutor") ExecutorService participantEventThreadPool) {
        this.participantEventThreadPool = participantEventThreadPool;
        this.userPushEventTaskCache = new ConcurrentHashMap<>();
    }

    /**
     * Submits a registration event task to the thread pool that executes that type of tasks
     * @param pushDetails- information about the push event
     */
    public void submitParticipantRegistrationEvent(ParticipantPushDetails pushDetails) {
        participantEventThreadPool.submit(() -> generateRegistrationEventTask(pushDetails));
    }

    /**
     * Generates a registration event task that does the following:
     * 1. Creates a folder named with the participant's name in the file system.
     * 2. Clones the participant's solutions repo inside his folder.
     * 3. Copies the testing framework inside the participant's folder.
     * 4. Copies the participant's solutions from its repo to the copied testing framework.
     * 5. Builds for the first time the modified local testing framework copy.
     * @param pushDetails- contains the information about the push event
     */
    private void generateRegistrationEventTask(ParticipantPushDetails pushDetails) {
        String participantGitHubUsername = pushDetails.getParticipantGitHubUsername();
        Executor executor = new DefaultExecutor();

        String participantDirectoryPathAndName = APP_WORKSPACE + participantGitHubUsername;
        executeCmd(generateCreateDirectoryCmd(participantDirectoryPathAndName), executor);
        executeCmd(generateCloneRepoCmd(participantDirectoryPathAndName, pushDetails), executor);
        executeCmd(generateCopyParentToParticipantDirectoryCmd(pushDetails), executor);
        executeCmd(generateCopyParticipantTasksToLocalParentCmd(pushDetails), executor);
        executeCmd(generateBuildLocalParentCmd(pushDetails), executor);
    }

    /**
     * Submits push event task to the thread pool, that executes that type of tasks, if there
     * is not another event for that participant executing already. If there is already event
     * executing for that participant it submits the submitParticipantPushEvent method to the thread pool.
     * @param pushDetails- contains the information about the push event
     */
    public void submitParticipantPushEvent(ParticipantPushDetails pushDetails) {
        String participantGitHubUsername = pushDetails.getParticipantGitHubUsername();

        userPushEventTaskCache.putIfAbsent(participantGitHubUsername, false);
        participantEventThreadPool.submit(() -> {
            if (userPushEventTaskCache.get(participantGitHubUsername)) {
                submitParticipantPushEvent(pushDetails);
            } else {
                generateParticipantPushEventTask(pushDetails);
            }
        });
    }

    /**
     * CHECK
     * Generates a registration event task that does the following:
     * - If there is no directory created for the participant on the file system:
     *    1. generateRegistrationEventTask() gets called.
     * -If there is directory existing for the participant on the file system:
     *    1. The latest changes for the participants repo gets pulled.
     *    2. Copies the participants solutions from its repo to the copied testing framework.
     *    3. The modified local testing framework copy, is ran by using bazel.
     * @param pushDetails- contains the information about the push event.
     */
    private void generateParticipantPushEventTask(ParticipantPushDetails pushDetails) {
        String participantGitHubUsername = pushDetails.getParticipantGitHubUsername();
        Executor executor = new DefaultExecutor();

        userPushEventTaskCache.replace(participantGitHubUsername, true);
        if (!checkIfDirectoryExists(APP_WORKSPACE + participantGitHubUsername, executor)) {
            generateRegistrationEventTask(pushDetails);
        }
        executeCmd(generatePullRepoCmd(pushDetails), executor);
        executeCmd(generateCopyParticipantTasksToLocalParentCmd(pushDetails), executor);
        executeCmd(generateRunLocalParentCmd(pushDetails), executor);
        userPushEventTaskCache.replace(participantGitHubUsername, false);
    }

    /**
     * @param path- presents the path to the directory
     * @param executor- the executor, that will be used for the execution, of the generated,
     * inside the method, command
     * @return boolean, that tells if the directory, that is passed as a parameter, exists.
     */
    private boolean checkIfDirectoryExists(String path, Executor executor) {
        int exitCode = executeCmd(generateChangeDirectoryCmd(path), executor);
        return exitCode == DEFAULT_SUCCESS_EXIT_CODE;
    }

    /**
     * Executes shell commands, passed as a parameter
     *
     * @param cmd- the shell command, that will get executed.
     * @param executor- the executor, that will be used for the execution, of the passed as a parameter, command.
     * @return integer, that presents the exit code, that is being returned, after the command has been executed.
     */
    private int executeCmd(CommandLine cmd, Executor executor) {
        int exitCode = DEFAULT_FAIL_EXIT_CODE;

        executor.setExitValue(DEFAULT_SUCCESS_EXIT_CODE);
        try {
            exitCode = executor.execute(cmd);
            System.out.println("Exit code: " + exitCode);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return exitCode;
    }

    /**
     * Generates a shell command, that copies the testing framework repo, to the participant's local directory.
     */
    private CommandLine generateCopyParentToParticipantDirectoryCmd(ParticipantPushDetails pushDetails) {
        String shellCommand = "cp -a " + APP_WORKSPACE + "parent/. "
                + APP_WORKSPACE + pushDetails.getParticipantGitHubUsername() + "/parent/";
        CommandLine cmd = new CommandLine("sh").addArgument("-c");
        cmd.addArgument(shellCommand, false);
        return cmd;
    }

    /**
     * Generates a shell command, that executes in the participant's directory, by copying the participant's
     * solutions, from its cloned repo, to the locally copied testing framework.
     */
    private CommandLine generateCopyParticipantTasksToLocalParentCmd(ParticipantPushDetails pushDetails) {
        String tasksPath = "src/main/java/test/parent";
        String participantGitHubUsername = pushDetails.getParticipantGitHubUsername();
        String participantGitHubRepoName = pushDetails.getParticipantGitHubRepoName();
        String hostGitHubRepoName = pushDetails.getHostGitHubRepoName();
        String shellCommand = "cp -a " + APP_WORKSPACE + participantGitHubUsername
                + "/" + participantGitHubRepoName + "/" + tasksPath + "/. "
                + APP_WORKSPACE + participantGitHubUsername + "/parent/" + hostGitHubRepoName + "/" + tasksPath + "/";
        CommandLine cmd = new CommandLine("sh").addArgument("-c");
        cmd.addArgument(shellCommand, false);
        return cmd;
    }

    /**
     * Generates a shell command, that creates a new directory.
     */
    private CommandLine generateCreateDirectoryCmd(String pathAndDirectoryName) {
        CommandLine cmdLine = new CommandLine("sh").addArgument("-c");
        cmdLine.addArgument("mkdir -p " + pathAndDirectoryName, false);
        return cmdLine;
    }

    /**
     * Generates a shell command, that clones the repo of the participant,
     * passed by the pushDetails parameter.
     */
    private CommandLine generateCloneRepoCmd(String path, ParticipantPushDetails pushDetails) {
        String hostGitHubUsername = pushDetails.getHostGitHubUsername();
        String hostGitHubAccessToken = pushDetails.getHostGitHubAccessToken();
        String participantGitHubRepoName = pushDetails.getParticipantGitHubRepoName();
        String participantGitHubUsername = pushDetails.getParticipantGitHubUsername();
        String shellCommand = "cd " + path + " && git clone "
                + "https://" + hostGitHubUsername + ":" + hostGitHubAccessToken
                + "@github.com/" + participantGitHubUsername + "/" + participantGitHubRepoName;
        CommandLine cmd = new CommandLine("sh").addArgument("-c");
        cmd.addArgument(shellCommand, false);
        return cmd;
    }

    /**
     * Generates a shell command, that pulls the repo of the participant,
     * passed by the pushDetails parameter.
     */
    private CommandLine generatePullRepoCmd(ParticipantPushDetails pushDetails) {
        String participantGitHubUsername = pushDetails.getParticipantGitHubUsername();
        String participantGitHubRepoName = pushDetails.getParticipantGitHubRepoName();
        String shellCommand = "cd " + APP_WORKSPACE + "/" + participantGitHubUsername + "/"
                + participantGitHubRepoName + " && git pull origin main";
        CommandLine cmd = new CommandLine("sh").addArgument("-c");
        cmd.addArgument(shellCommand, false);
        return cmd;
    }

    /**
     * Generates a shell command, that builds, using bazel, the modified testing framework,
     * that is located in the participant's directory.
     */
    private CommandLine generateBuildLocalParentCmd(ParticipantPushDetails pushDetails) {
        String jvmOptions = "-XX:TieredStopAtLevel=1";
        String shellCommand = "cd " + APP_WORKSPACE + pushDetails.getParticipantGitHubUsername()
                + "/parent/" + pushDetails.getHostGitHubRepoName()
                + " && bazel --max_idle_secs=0 build --jvmopt=\"" + jvmOptions + "\" //src/test/java:tests";
        CommandLine cmd = new CommandLine("sh").addArgument("-c");
        cmd.addArgument(shellCommand, false);
        return cmd;
    }

    /**
     * Generates a shell command, that runs, using bazel, the modified testing framework,
     * that is located in the participant's directory.
     */
    private CommandLine generateRunLocalParentCmd(ParticipantPushDetails pushDetails) {
        String jvmOptions = "-XX:TieredStopAtLevel=1";
        String shellCommand = "cd " + APP_WORKSPACE + pushDetails.getParticipantGitHubUsername()
                + "/parent/" + pushDetails.getHostGitHubRepoName()
                + " && bazel --max_idle_secs=0 run --nofetch --jvmopt=\"" + jvmOptions + "\" //src/test/java:tests";
        CommandLine cmd = new CommandLine("sh").addArgument("-c");
        cmd.addArgument(shellCommand, false);
        return cmd;
    }

    /**
     * Generates a shell command, that is used for changing a directory
     */
    private CommandLine generateChangeDirectoryCmd(String path) {
        String shellCommand = "cd " + path;
        CommandLine cmd = new CommandLine("sh").addArgument("-c");
        cmd.addArgument(shellCommand, false);
        return cmd;
    }

}
