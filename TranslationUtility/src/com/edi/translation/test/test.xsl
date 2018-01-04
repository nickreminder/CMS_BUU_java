
<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:cmn="http://gov.hhs.cms.hix.dsh"
	xmlns:bem="http://bem.dsh.cms.gov" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xmlns:java="http://xml.apache.org/xslt/java" exclude-result-prefixes="java"
	xmlns:BemUtil="com.edi.bemtranslation.BemUtil">

<xsl:output method="text" encoding="utf-8" />
	<xsl:template match="/">
		<xsl:param name="fileName" />
		<xsl:for-each select="document($fileName)/bem:BenefitEnrollmentRequest/bem:BenefitEnrollmentMaintenance">
			<xsl:value-of select="bem:TransactionInformation/bem:CurrentTimeStamp" />
			<xsl:variable name="xmlDateTime" select="bem:TransactionInformation/bem:CurrentTimeStamp" />
		</xsl:for-each>
	</xsl:template>

</xsl:stylesheet>