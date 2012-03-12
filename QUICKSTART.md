#
## git clone "tutorial"
# We will show you here how to get tap code from github, build it and run a wordcount sample
# 

mkdir tap_0.2.0
git clone -b tap_0.2.0 git@github.com:tap-git/tap.git
cd tap
mvn clean install
cd samples-phase-2
mvn clean install

hadoop fs -mkdir /tmp/inputs
hadoop fs -put ../share/*.txt /tmp/inputs
hadoop jar target/tap-samples-1.0-SNAPSHOT.jar tap.sample.WordCount -i /tmp/inputs -o /tmp/wcoutput

hadoop fs -get /tmp/wcoutput/part-00000 .
cat part-00000 .