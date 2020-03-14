package object v1 {
  val BasePath = "/your/project/base/path"
  val SparkLocalDir: String = BasePath + "/tmp"
  val TeraUrl = "jdbc:teradata://your.tera.server/TMODE=TERA,CHARSET=UTF8,TYPE=FASTEXPORT"
  val TeraDriverClass = "com.teradata.jdbc.TeraDriver"
  val TeraUser = "username"
  val TeraPassword = "password"
  val TeraExecutorPath = "/path/of/executor.sh"
  val DemoUser = "demome"
  val DemoUserID = 1L
  val EmailSuffix = "@domain.com"
  val ParquetPath: String = BasePath + "/easyrpt"
  val LdapUrl = "ldaps://your.ladp.server:636"
}
