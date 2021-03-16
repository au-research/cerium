<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:igsn="https://identifiers.ardc.edu.au/schemas/ardc-igsn-desc" version="2.0"
	exclude-result-prefixes="igsn">
	<xsl:output indent="yes" method="xml"/>
	<xsl:param name="registrantName" select="'registrantName'"/>
	<xsl:param name="nameIdentifier" select="'nameIdentifier'"/>
	<xsl:param name="nameIdentifierScheme" select="'nameIdentifierScheme'"/>
	<xsl:param name="timeStamp" select="'2018-06-06T13:45:45.5654'"/>
	<!--
    XSLT transformation to create registration metadata v1.1 from an IGSN CSIRO v3.0 record
    -->

	<xsl:template match="/">
		<xsl:apply-templates select="igsn:resources/igsn:resource"/>
	</xsl:template>

	<xsl:template match="igsn:resource">
		<sample xmlns="http://igsn.org/schema/kernel-v.1.0"
			xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			xsi:schemaLocation="http://igsn.org/schema/kernel-v.1.0 http://doidb.wdc-terra.org/igsn/schemas/igsn.org/schema/1.0/igsn.xsd">
			<xsl:apply-templates select="igsn:resourceIdentifier"/>
			<xsl:if test="$registrantName != 'registrantName'">
				<xsl:element name="registrant">
					<xsl:element name="registrantName">
						<xsl:value-of select="$registrantName"/>
					</xsl:element>
					<xsl:if
						test="$nameIdentifier != 'nameIdentifier' and $nameIdentifierScheme != 'nameIdentifierScheme'">
						<xsl:element name="nameIdentifier">
							<xsl:attribute name="nameIdentifierScheme">					
								<xsl:value-of select="$nameIdentifierScheme"/>
							</xsl:attribute> 
							<xsl:value-of select="$nameIdentifier"/>
						</xsl:element>
					</xsl:if>
				</xsl:element>
			</xsl:if>

			<xsl:apply-templates select="igsn:relatedResources"/>

			<!-- registered, submitted updated deprecated destroyed -->
			<xsl:element name="log" xmlns="http://igsn.org/schema/kernel-v.1.0">
				<xsl:apply-templates select="igsn:logDate"/>
			</xsl:element>

		</sample>
	</xsl:template>

	<xsl:template match="igsn:resourceIdentifier">
		<xsl:element name="sampleNumber" xmlns="http://igsn.org/schema/kernel-v.1.0">
			<xsl:attribute name="identifierType"><xsl:text>igsn</xsl:text></xsl:attribute>
			<xsl:value-of select="."/>
		</xsl:element>
	</xsl:template>

	<xsl:template match="igsn:relatedResources">
		<xsl:element name="relatedResourceIdentifiers"
			xmlns="http://igsn.org/schema/kernel-v.1.0">
			<xsl:apply-templates select="igsn:relatedResource"/>
		</xsl:element>
	</xsl:template>

	<xsl:template match="igsn:relatedResource">
		<xsl:element name="relatedIdentifier"
			xmlns="http://igsn.org/schema/kernel-v.1.0">
			<xsl:apply-templates select="igsn:relatedResourceIdentifier/@relatedResourceIdentifierType"/>
			<xsl:apply-templates select="@relationType"/>

			<xsl:value-of select="normalize-space(igsn:relatedResourceIdentifier)"/>
		</xsl:element>
	</xsl:template>

	<xsl:template match="@relatedResourceIdentifierType">
		<!-- 
			https://doidb.wdc-terra.org//igsn/schemas/igsn.org/schema/1.0/include/igsn-relatedIdentifierType-v1.0.xsd
		TODO: Need to map to the following values 
		The type of related identifier supported
		<xs:enumeration value="doi"/>
		<xs:enumeration value="handle"/>
		<xs:enumeration value="lsid"/>
		<xs:enumeration value="url"/>
		<xs:enumeration value="urn"/>
		-->
		<!--xsl:attribute name="relatedIdentifierType">
			<xsl:value-of select="."/>
		</xsl:attribute-->
	</xsl:template>
	
	

	<xsl:template match="@relationType">
		<!-- 
	TODO: Need to map to the following values 
	https://doidb.wdc-terra.org//igsn/schemas/igsn.org/schema/1.0/include/igsn-relationType-v1.0.xsd
	<xs:enumeration value="IsCitedBy"/>
	<xs:enumeration value="IsPartOf"/>	
	<xs:enumeration value="HasPart"/>	
	<xs:enumeration value="IsReferencedBy"/>	
	<xs:enumeration value="References"/>	
	<xs:enumeration value="IsDocumentedBy"/>	
	<xs:enumeration value="Documents"/>	
	<xs:enumeration value="IsCompiledBy"/>	
	<xs:enumeration value="Compiles"/>	
	<xs:enumeration value="IsVariantFormOf"/>
	<xs:enumeration value="IsOriginalFormOf"/>
	-->
		<!--xsl:attribute name="relationType">
			<xsl:value-of select="."/>
		</xsl:attribute-->
	</xsl:template>


	<xsl:template match="igsn:logDate">
		<xsl:element name="logElement" xmlns="http://igsn.org/schema/kernel-v.1.0">
			<xsl:attribute name="event">
				<xsl:value-of select="@eventType"/>
			</xsl:attribute>
			<xsl:attribute name="timeStamp">
				<xsl:value-of select="$timeStamp"/>
			</xsl:attribute>
		</xsl:element>
	</xsl:template>

</xsl:stylesheet>
