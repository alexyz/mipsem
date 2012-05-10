/*
 * Generate library constants for the emulator to load at run time
 */
#include <linux/types.h>
#include <linux/dirent.h>
#include <linux/unistd.h>
#include <sys/time.h>
#include <sys/types.h>
/* need this to get stat64, check the glibc readme */
#define __USE_LARGEFILE64
#include <sys/stat.h>
#include <sys/mman.h>
#include <sys/uio.h>
#include <sys/utsname.h>
#include <sys/ioctl.h>
#include <sys/wait.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <time.h>
#include <stddef.h>
#include <unistd.h>
#include <stdio.h>
#include <fcntl.h>
#include <signal.h>
#include <errno.h>
#include <termios.h>
#include <signal.h>

int main() {

  /* sizeof struct */
  #define S(A) printf("sizeof_" #A "=%d\n", sizeof A)
  /* constant value */
  #define C(A) printf(#A "=%d\n", (int) A)
  #define C2(A,B) printf(#A "=%d,%d\n", (int) A, (int) B)
  /* offset and size of struct field */
  #define F(A, B) \
    printf(#A "." #B "=%d,%d\n", offsetof(struct A, B), sizeof A.B);
  #define AF(A, B) \
    printf(#A "." #B "=%d,%d,%d,%d\n", \
      offsetof(struct A, B), sizeof A.B, sizeof A.B[0], sizeof A.B / sizeof A.B[0]);
  
  /* open modes */
  C(O_RDONLY);
  C(O_WRONLY);
  C(O_RDWR);
  C(O_APPEND);
  C(O_CREAT);
  C(O_TRUNC);
  C(O_EXCL);
  C(O_ASYNC);
  //C(O_DIRECT);
  //C(O_DIRECTORY);
  C(O_LARGEFILE);
  //C(O_NOATIME);
  C(O_NOCTTY);
  //C(O_NOFOLLOW);
  C(O_NONBLOCK);
  //C(O_NDELAY);
  C(O_SYNC);
  
  /* file permissions */
  C(S_IFSOCK);
  C(S_IFLNK);
  C(S_IFREG);
  C(S_IFBLK);
  C(S_IFDIR);
  C(S_IFCHR);
  C(S_IFIFO);
  C(S_ISUID);
  C(S_ISGID);
  C(S_ISVTX);
  C(S_IRWXU);
  C(S_IRUSR);
  C(S_IWUSR);
  C(S_IXUSR);
  C(S_IRWXG);
  C(S_IRGRP);
  C(S_IWGRP);
  C(S_IXGRP);
  C(S_IRWXO);
  C(S_IROTH);
  C(S_IWOTH);
  C(S_IXOTH);
  
  /* fcntl duplicate fd */
  C(F_DUPFD);
  /* fcntl get close on exec flag */
  C(F_GETFD);
  /* fcntl set close on exec flag */
  C(F_SETFD);
  C(FD_CLOEXEC);
  /* fcntl get file flags */
  C(F_GETFL);
  /* fcntl set file flags */
  C(F_SETFL);
  
  /** misc errors */
  C(EPERM);
  C(ENOENT);
  C(ESRCH);
  C(EINTR);
  C(EIO);
  C(ENXIO);
  C(E2BIG);
  C(ENOEXEC);
  C(EBADF);

  C(ECHILD);
  C(EAGAIN);
  C(ENOMEM);
  C(EACCES);
  C(EFAULT);
  C(ENOTBLK);
  C(EBUSY);
  C(EEXIST);
  C(EXDEV);
  C(ENODEV);

  C(ENOTDIR);
  C(EISDIR);
  C(EINVAL);
  C(ENFILE);
  C(EMFILE);
  C(ENOTTY);
  C(ETXTBSY);
  C(EFBIG);
  C(ENOSPC);
  C(ESPIPE);
  C(EROFS);
  C(EMLINK);
  C(EPIPE);
  C(EDOM);
  C(ERANGE);

  /* extended errors */

  C(ENOSYS);
  C(EISCONN);
  C(ECONNREFUSED);
  C(ENOTSOCK);
  
  /* seek mode */
  C(SEEK_SET);
  C(SEEK_CUR);
  C(SEEK_END);

  /* mmap */
  C(PROT_EXEC);
  C(PROT_READ);
  C(PROT_WRITE);
  C(PROT_NONE);
  C(MAP_FIXED);
  C(MAP_SHARED);
  C(MAP_PRIVATE);
  C(MAP_ANONYMOUS); /* map /dev/zero */
  C(MAP_FAILED);
  
  struct stat stat;

  S(stat);
  F(stat, st_dev);
  F(stat, st_ino);
  F(stat, st_nlink);
  F(stat, st_uid);
  F(stat, st_gid);
  F(stat, st_rdev);
  F(stat, st_mode);
  F(stat, st_size);
  F(stat, st_blksize);
  F(stat, st_blocks);
  F(stat, st_atime);
  F(stat, st_mtime);
  F(stat, st_ctime);

  struct iovec iovec;

  S(iovec);
  F(iovec, iov_base);
  F(iovec, iov_len);

  struct stat64 stat64;

  S(stat64);
  F(stat64, st_dev);
  F(stat64, st_ino);
  F(stat64, st_nlink);
  F(stat64, st_uid);
  F(stat64, st_gid);
  F(stat64, st_rdev);
  F(stat64, st_mode);
  F(stat64, st_size);
  F(stat64, st_blksize);
  F(stat64, st_blocks);
  F(stat64, st_atime);
  F(stat64, st_mtime);
  F(stat64, st_ctime);

  struct utsname utsname;
  S(utsname);
  F(utsname, sysname);
  F(utsname, nodename);
  F(utsname, release);
  F(utsname, version);
  F(utsname, machine);

  struct sigaction sigaction;
  S(sigaction);
  F(sigaction, sa_handler);
  F(sigaction, sa_sigaction);
  F(sigaction, sa_mask);
  F(sigaction, sa_flags);
  F(sigaction, sa_restorer);
  C(SIG_DFL);
  C(SIG_IGN);

  /* top level ioctls, not yet used */
  C(FIOCLEX);
  C(FIONCLEX);
  C(FIONBIO);
  C(FIOASYNC);
  C(FIOQSIZE);

  /* terminal size ioctl */
  C(TIOCGWINSZ);
  /* terminal controlling process */
  C(TIOCGPGRP);
  C(TIOCSPGRP);

  struct winsize winsize;
  S(winsize);
  F(winsize, ws_row);
  F(winsize, ws_col);
  F(winsize, ws_xpixel);
  F(winsize, ws_ypixel);

  /* ioctl constants */
  C(TCGETA);
  C(TCSETA);
  C(TCSETAW);
  C(TCSETAF);
  C(TCSBRK);
  C(TCXONC);
  C(TCFLSH);
  C(TCGETS);
  C(TCSETS);
  C(TCSETSW);
  C(TCSETSF);
  C(TIOCEXCL);
  C(TIOCNXCL);
  C(TIOCOUTQ);
  C(TIOCSTI);
  C(TIOCMGET);
  C(TIOCMBIS);
  C(TIOCMBIC);
  C(TIOCMSET);
  C(TIOCPKT);

  /* termios constants */
  C(BRKINT);
  C(ICRNL);
  C(IGNBRK);
  C(IGNCR);
  C(IGNPAR);
  C(INLCR);
  C(INPCK);
  C(ISTRIP);
  C(IXANY);
  C(IXOFF);
  C(IXON);
  C(PARMRK);

  C(OPOST);
  C(ONLCR);
  C(OCRNL);
  C(ONOCR);
  C(ONLRET);
  C(OFILL);
  C(OFDEL);
  C(NLDLY);
  C(NL0);
  C(NL1);
  C(CRDLY);
  C(CR0);
  C(CR1);
  C(CR2);
  C(CR3);
  C(TABDLY);
  C(TAB0);
  C(TAB1);
  C(TAB2);
  C(TAB3);
  C(BSDLY);
  C(BS0);
  C(BS1);
  C(VTDLY);
  C(VT0);
  C(VT1);
  C(FFDLY);
  C(FF0);
  C(FF1);


  C(CLOCAL);
  C(CREAD);
  C(CSIZE);
  C(CS5);
  C(CS6);
  C(CS7);
  C(CS8);
  C(CSTOPB);
  C(HUPCL);
  C(PARENB);
  C(PARODD);	

  C(B0);
  C(B600);
  C(B50);
  C(B1200);
  C(B75);
  C(B1800);
  C(B110);
  C(B2400);
  C(B134);
  C(B4800);
  C(B150);
  C(B9600);
  C(B200);
  C(B19200);
  C(B300);
  C(B38400);

  C(ECHO);
  C(ECHOE);
  C(ECHOK);
  C(ECHONL);
  C(ICANON);
  C(IEXTEN);
  C(ISIG);
  C(NOFLSH);
  C(TOSTOP);

  /* unused so far */
  struct termios termios;
  S(termios);
  F(termios, c_iflag);
  F(termios, c_oflag);
  F(termios, c_cflag);
  F(termios, c_lflag);
  C(NCCS); /* 23... even though array is length 32 */
  AF(termios, c_cc);

  struct timespec timespec;
  S(timespec);
  F(timespec, tv_sec);
  F(timespec, tv_nsec);

  struct dirent dirent;
  S(dirent);
  F(dirent, d_ino);
  F(dirent, d_off);
  F(dirent, d_reclen);
  //F(dirent, d_type);
  F(dirent, d_name);

  struct dirent64 dirent64;
  S(dirent64);
  F(dirent64, d_ino);
  F(dirent64, d_off);
  F(dirent64, d_reclen);
  F(dirent64, d_type);
  F(dirent64, d_name);

  C(WNOHANG);
  C(WUNTRACED);

  struct timeval timeval;
  S(timeval);
  F(timeval, tv_sec);
  F(timeval, tv_usec);

  struct timezone timezone;
  S(timezone);
  F(timezone, tz_minuteswest);
  F(timezone, tz_dsttime);

  // signal numbers plus the cfern default action
  // TODO add a noblock flag
  // 0=exit 1=ignore 2=abort 3=stop 4=cont
  #define ST 0
  #define SI 1
  #define SA 2
  #define SS 3
  #define SC 4
  C2(SIGHUP, ST);
  C2(SIGINT, ST);
  C2(SIGQUIT, SA);
  C2(SIGILL, SA);
  C2(SIGTRAP, SA);
  C2(SIGABRT, SA);
  C2(SIGEMT, ST);
  C2(SIGFPE, SA);
  C2(SIGKILL, ST);
  C2(SIGBUS, SA);
  C2(SIGSEGV, SA);
  C2(SIGSYS, SA);
  C2(SIGPIPE, ST);
  C2(SIGALRM, ST);
  C2(SIGTERM, ST);
  C2(SIGUSR1, ST);
  C2(SIGUSR2, ST);
  C2(SIGCHLD, SI);
  C2(SIGPWR, SI);
  C2(SIGWINCH, SI);
  C2(SIGURG, SI);
  C2(SIGIO, SI);
  C2(SIGSTOP, SS);
  C2(SIGTSTP, SS);
  C2(SIGCONT, SC);
  C2(SIGTTIN, SS);
  C2(SIGTTOU, SS);
  C2(SIGVTALRM, ST);
  C2(SIGPROF, ST);
  C2(SIGXCPU, SA);
  C2(SIGXFSZ, SA);

  // options for sigprocmask
  C(SIG_BLOCK);
  C(SIG_UNBLOCK);
  C(SIG_SETMASK);

  // struct sigaction.sa_flags
  C(SA_NOCLDSTOP);
  C(SA_NOCLDWAIT);
  C(SA_RESETHAND);
  C(SA_ONSTACK);
  C(SA_RESTART);
  C(SA_NODEFER);
  C(SA_SIGINFO);

  // struct sigset_t is in linux/include/asm-mips/signal.h
  // but seems to be different in glibc
  // we only read the first word anyway (each bit represents a signal)

  C(PF_INET);
  // the next three are equal
  //C(PF_FILE);
  //C(PF_UNIX);
  C(PF_LOCAL);
  C(PF_INET6);

  C(SOCK_STREAM);
  C(SOCK_DGRAM);
  C(SOCK_SEQPACKET);
  C(SOCK_RAW);
  C(SOCK_RDM);
  C(SOCK_PACKET);

  C(SOL_SOCKET);
  C(IPPROTO_TCP);
  C(SO_ACCEPTCONN);
  C(SO_BROADCAST);
  C(SO_DEBUG);
  C(SO_DONTROUTE);
  C(SO_ERROR);
  C(SO_KEEPALIVE);
  C(SO_LINGER);
  C(SO_OOBINLINE);
  C(SO_RCVBUF);
  C(SO_RCVLOWAT);
  C(SO_RCVTIMEO);
  C(SO_REUSEADDR);
  C(SO_SNDBUF);
  C(SO_SNDLOWAT);
  C(SO_SNDTIMEO);
  C(SO_TYPE);

  // include/bits/socket.h
  struct sockaddr sockaddr;
  S(sockaddr);
  F(sockaddr, sa_family);
  F(sockaddr, sa_data);

  // include/linux/in.h
  struct sockaddr_in sockaddr_in;
  S(sockaddr_in);
  F(sockaddr_in, sin_family);
  F(sockaddr_in, sin_port);
  F(sockaddr_in, sin_addr); // is a struct in_addr
  
  struct in_addr in_addr;
  S(in_addr);
  F(in_addr, s_addr);

  C(VINTR);
  C(VQUIT);
  C(VERASE);
  C(VKILL);
  C(VMIN);
  C(VTIME);
  C(VEOL2);
  C(VSWTC);
  C(VSTART);
  C(VSTOP);
  C(VSUSP);
  C(VREPRINT);
  C(VDISCARD);
  C(VWERASE);
  C(VLNEXT);
  C(VEOF);
  C(VEOL);

  /* array of 1024 bits */
  fd_set fd_set;
  S(fd_set);

  return 0;
}
