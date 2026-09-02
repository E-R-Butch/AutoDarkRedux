package me.ranko.autodark.Exception

/**
 * Signals that an error occurred during execute command.
 *
 * @author  0ranko0P
 * @see     Runtime.exec
 */
class CommandExecuteError : Exception {
    /**
     * Constructs an [CommandExecuteError] with `null`
     * as its error detail message.
     */
    constructor() : super()

    /**
     * Constructs a new CommandExecuteError with the specified detail message. The
     * cause is not initialized, and may subsequently be initialized by
     * a call to [.initCause].
     *
     * @param   message   the detail message. The detail message is saved for
     *          later retrieval by the [.getMessage] method.
     */
    constructor(message: String?) : super(message)

    /**
     * Constructs an [CommandExecuteError] with the specified detail message
     * and cause.
     *
     * Note that the detail message associated with `cause` is
     * *not* automatically incorporated into this exception's detail
     * message.
     *
     * @param message
     *        The detail message (which is saved for later retrieval
     *        by the [.getMessage] method)
     *
     * @param cause
     *        The cause (which is saved for later retrieval by the
     *        [.getCause] method).  (A null value is permitted,
     *        and indicates that the cause is nonexistent or unknown.)
     *
     */
    constructor(message: String?, cause: Throwable?) : super(message, cause)

    /**
     * Constructs an [CommandExecuteError] with the specified cause and a
     * detail message of `(cause==null ? null : cause.toString())`
     * (which typically contains the class and detail message of `cause`).
     * This constructor is useful for IO exceptions that are little more
     * than wrappers for other throwables.
     *
     * @param cause
     *        The cause (which is saved for later retrieval by the
     *        [.getCause] method).  (A null value is permitted,
     *        and indicates that the cause is nonexistent or unknown.)
     *
     */
    constructor(cause: Throwable?) : super(cause)

    companion object {
        private const val serialVersionUID: Long = -418375825643090127L
    }
}
