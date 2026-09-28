// Drop-in replacement for Termux's libtermux.so (com.termux.terminal.JNI).
// The prebuilt library in the terminal-emulator AAR is 4 KB page aligned and
// cannot be loaded on 16 KB page kernels, so the app ships this build instead.
#define _GNU_SOURCE
#include <jni.h>
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <termios.h>
#include <unistd.h>

static int throw_runtime_exception(JNIEnv *env, const char *message) {
    jclass exception = (*env)->FindClass(env, "java/lang/RuntimeException");
    (*env)->ThrowNew(env, exception, message);
    return -1;
}

static char **to_c_array(JNIEnv *env, jobjectArray array) {
    if (array == NULL) return NULL;
    jsize size = (*env)->GetArrayLength(env, array);
    char **result = calloc((size_t)size + 1, sizeof(char *));
    if (result == NULL) return NULL;
    for (jsize i = 0; i < size; i++) {
        jstring value = (jstring)(*env)->GetObjectArrayElement(env, array, i);
        const char *utf = (*env)->GetStringUTFChars(env, value, NULL);
        result[i] = strdup(utf);
        (*env)->ReleaseStringUTFChars(env, value, utf);
        (*env)->DeleteLocalRef(env, value);
    }
    return result;
}

static void free_c_array(char **array) {
    if (array == NULL) return;
    for (char **item = array; *item; item++) free(*item);
    free(array);
}

JNIEXPORT jint JNICALL
Java_com_termux_terminal_JNI_createSubprocess(JNIEnv *env, jclass clazz, jstring java_cmd,
                                              jstring java_cwd, jobjectArray java_args,
                                              jobjectArray java_env, jintArray process_id_array,
                                              jint rows, jint columns, jint cell_width,
                                              jint cell_height) {
    (void)clazz;
    int master = posix_openpt(O_RDWR | O_CLOEXEC);
    if (master < 0) return throw_runtime_exception(env, "Cannot open /dev/ptmx");
    char slave_name[64];
    if (grantpt(master) || unlockpt(master) || ptsname_r(master, slave_name, sizeof(slave_name))) {
        close(master);
        return throw_runtime_exception(env, "Cannot grantpt()/unlockpt()/ptsname_r() on /dev/ptmx");
    }

    // UTF-8 input and no XON/XOFF, so Ctrl+S/Ctrl+Q reach the TUI.
    struct termios tios;
    tcgetattr(master, &tios);
    tios.c_iflag |= IUTF8;
    tios.c_iflag &= (tcflag_t)~(IXON | IXOFF);
    tcsetattr(master, TCSANOW, &tios);

    struct winsize size = {
        .ws_row = (unsigned short)rows,
        .ws_col = (unsigned short)columns,
        .ws_xpixel = (unsigned short)(columns * cell_width),
        .ws_ypixel = (unsigned short)(rows * cell_height),
    };
    ioctl(master, TIOCSWINSZ, &size);

    const char *cmd_utf = (*env)->GetStringUTFChars(env, java_cmd, NULL);
    const char *cwd_utf = (*env)->GetStringUTFChars(env, java_cwd, NULL);
    char *cmd = strdup(cmd_utf);
    char *cwd = strdup(cwd_utf);
    (*env)->ReleaseStringUTFChars(env, java_cmd, cmd_utf);
    (*env)->ReleaseStringUTFChars(env, java_cwd, cwd_utf);
    char **argv = to_c_array(env, java_args);
    char **envp = to_c_array(env, java_env);

    pid_t pid = fork();
    if (pid < 0) {
        free(cmd); free(cwd); free_c_array(argv); free_c_array(envp);
        close(master);
        return throw_runtime_exception(env, "Fork failed");
    }
    if (pid == 0) {
        // Clear signal mask and dispositions inherited from the ART runtime.
        sigset_t signals;
        sigfillset(&signals);
        sigprocmask(SIG_UNBLOCK, &signals, NULL);
        for (int sig = 1; sig < NSIG; sig++) signal(sig, SIG_DFL);

        close(master);
        setsid();
        int slave = open(slave_name, O_RDWR);
        if (slave < 0) {
            dprintf(STDERR_FILENO, "Cannot open pty slave: %s\n", strerror(errno));
            _exit(1);
        }
        dup2(slave, STDIN_FILENO);
        dup2(slave, STDOUT_FILENO);
        dup2(slave, STDERR_FILENO);

        DIR *fds = opendir("/proc/self/fd");
        if (fds != NULL) {
            int dir_fd = dirfd(fds);
            struct dirent *entry;
            while ((entry = readdir(fds)) != NULL) {
                int fd = atoi(entry->d_name);
                if (fd > STDERR_FILENO && fd != dir_fd) close(fd);
            }
            closedir(fds);
        }

        clearenv();
        if (envp) for (char **item = envp; *item; item++) putenv(*item);
        if (chdir(cwd) != 0) {
            // A missing directory must not block the session from starting.
            chdir("/");
        }
        execvp(cmd, argv);
        dprintf(STDERR_FILENO, "Exec of \"%s\" failed: %s\n", cmd, strerror(errno));
        _exit(1);
    }

    free(cmd); free(cwd); free_c_array(argv); free_c_array(envp);
    jint *process_id = (*env)->GetPrimitiveArrayCritical(env, process_id_array, NULL);
    if (process_id) {
        process_id[0] = pid;
        (*env)->ReleasePrimitiveArrayCritical(env, process_id_array, process_id, 0);
    }
    return master;
}

JNIEXPORT void JNICALL
Java_com_termux_terminal_JNI_setPtyWindowSize(JNIEnv *env, jclass clazz, jint fd, jint rows,
                                              jint cols, jint cell_width, jint cell_height) {
    (void)env; (void)clazz;
    struct winsize size = {
        .ws_row = (unsigned short)rows,
        .ws_col = (unsigned short)cols,
        .ws_xpixel = (unsigned short)(cols * cell_width),
        .ws_ypixel = (unsigned short)(rows * cell_height),
    };
    ioctl(fd, TIOCSWINSZ, &size);
}

JNIEXPORT jint JNICALL
Java_com_termux_terminal_JNI_waitFor(JNIEnv *env, jclass clazz, jint pid) {
    (void)env; (void)clazz;
    int status;
    waitpid(pid, &status, 0);
    if (WIFEXITED(status)) return WEXITSTATUS(status);
    if (WIFSIGNALED(status)) return -WTERMSIG(status);
    return 0;
}

JNIEXPORT void JNICALL
Java_com_termux_terminal_JNI_close(JNIEnv *env, jclass clazz, jint fd) {
    (void)env; (void)clazz;
    close(fd);
}
