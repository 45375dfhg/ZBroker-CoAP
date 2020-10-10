package zio.nio

import zio.nio.core.InetAddress

/**
 * This class extends the zio.nio library implementation to provide a functional equal() and hashCode() method.
 * The latest release (RC-9) of zio.nio does not provide these but their implementations can already be found
 * inside the library's repository. As soon as a new release candidate is available, this implementation can be removed.
 * @param inetAddress A java.net InetAddress that is automatically extracted from the given zio.nio
 *                    InetAddress.
 */
class InetAddress_(val inetAddress: InetAddress) extends InetAddress(inetAddress.jInetAddress) {

  override def equals(obj: Any): Boolean =
    obj match {
      case other: InetAddress => inetAddress.jInetAddress.equals(other.jInetAddress)
      case _                  => false
    }

  override def hashCode(): Int = inetAddress.jInetAddress.hashCode()
}
