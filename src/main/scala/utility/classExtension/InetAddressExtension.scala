package utility.classExtension

import java.util

import zio.nio.InetAddress_
import zio.nio.core.InetAddress

object InetAddressExtension {

  implicit class InetAddressExtension(inetAddress: InetAddress) {

    def equalsN(obj: Any): Boolean =
      obj match {
        case other: InetAddress => inetAddress.address.sameElements(other.address)
        case _ => false
      }

    val hashCodeN: Int = util.Arrays.hashCode(inetAddress.address)

    val ex = new InetAddress_(inetAddress)
  }

}
