java -Dwebservice.home=$AXIS_HOME/ -Dprocess.name=NcbiBlastProcessor \
-cp $GUS_HOME/lib/java/ApiComplexa-WuBlast.jar:\
$GUS_HOME/lib/java/ApiComplexa-NcbiBlast.jar:\
$GUS_HOME/lib/java/junit.jar:\
$GUS_HOME/lib/java/WDK-Service.jar:\
$GUS_HOME/lib/java/WDK-Model.jar \
junit.textui.TestRunner org.gusdb.wdk.service.test.WdkProcessServiceTest
