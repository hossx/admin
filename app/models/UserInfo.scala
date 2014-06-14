package models

case class UserInfo(id: Long, email: String, emailVerified: Boolean,
  mobileVerified: Boolean, status: String)
