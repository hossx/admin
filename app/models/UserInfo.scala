package models

case class UserInfo(id: Long, email: String, emailVerified: Boolean,
  mobile: String, mobileVerified: Boolean,  realName: String, status: String)
