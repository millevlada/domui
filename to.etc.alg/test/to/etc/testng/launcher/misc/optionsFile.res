-root /home/vmijic/Data/Projects/Itris/Viewpoint/bzr/vp-split-5.0-webdriver/vp-5.0
-m2.repo /home/vmijic/.m2
-project vp-selenium-webdriver-tests
-parallel CLASS
-threads 5
-testng.reporter nl.itris.vp.webdriver.core.report.WdVpTestXMLReporter
-testng.reporter.root /home/vmijic/Data/Projects/Itris/Viewpoint/testing/tests/proba/vp-suite
-testng.browser firefox
-testng.remote.hub local
-testng.server.url http://localhost:8080/ITRIS_VO02/
-testng.username vpc
-testng.password rhijnspoor
-include.package nl.itris.vp.webdriver.viewpoint.tests.bae.correctionBooking
-testng.wait.timeout 40000
