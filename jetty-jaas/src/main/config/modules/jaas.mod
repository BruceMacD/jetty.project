#
# JAAS Feature
#

[depend]
server

[lib]
# JAAS jars
lib/jetty-jaas-${jetty.version}.jar

[xml]
# JAAS configuration
etc/jetty-jaas.xml

[ini]
jaas.login.conf=etc/login.conf