<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:igsn="https://identifiers.ardc.edu.au/schemas/ardc-igsn-desc" version="2.0"
    exclude-result-prefixes="igsn">
    <xsl:output indent="yes" method="xml"/>

    <!-- 
XSLT to convert ardc IGSN v.1 metadata to oai_dc format

-->

    <xsl:variable name="handle-url" select="'http://hdl.handle.net/'"/>
    <xsl:variable name="coverage-pre-text" select="'Coordinates(lon/Lat):'"/>
    <xsl:template match="/">
        <xsl:apply-templates select="igsn:resources/igsn:resource"/>
    </xsl:template>

    <xsl:template match="igsn:resource">
        <dc xmlns="http://www.openarchives.org/OAI/2.0/oai_dc/"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">
            <xsl:call-template name="title"/>
            <xsl:for-each select="igsn:curationDetails/igsn:curation/igsn:curator">
                <xsl:call-template name="publisher"/>
            </xsl:for-each>
            <xsl:for-each select="igsn:date/igsn:timePeriod/igsn:start">
                <xsl:call-template name="date"/>
            </xsl:for-each>
            <xsl:for-each select="igsn:comments">
                <xsl:call-template name="description"/>
            </xsl:for-each>


            <xsl:for-each select="igsn:classifications/igsn:classification">
                <xsl:call-template name="subject"/>
            </xsl:for-each>
            <xsl:for-each select="igsn:resourceTypes/igsn:resourceType">
                <xsl:call-template name="type"/>
            </xsl:for-each>

            <xsl:for-each select="igsn:resourceIdentifier">
                <xsl:call-template name="identifier"/>
            </xsl:for-each>
            <xsl:for-each select="igsn:location/igsn:locality">
                <xsl:call-template name="locality"/>
            </xsl:for-each>
            <xsl:for-each select="igsn:location/igsn:geometry">
                <xsl:call-template name="coverage"/>
            </xsl:for-each>

            <xsl:for-each select="igsn:contributors/igsn:contributor/igsn:contributorName">
                <xsl:call-template name="creator"/>
            </xsl:for-each>
            <xsl:for-each
                select="igsn:relatedResources/igsn:relatedResource/igsn:relatedResourceIdentifier">
                <xsl:call-template name="relation"/>
            </xsl:for-each>

            <xsl:for-each select="igsn:materialTypes/igsn:materialType">
                <xsl:call-template name="format"/>
            </xsl:for-each>

            <!-- TODO: NOT YET MAPPED -->
            <!--xsl:call-template name="source"/-->
            <!--xsl:call-template name="language"/-->
            <!--xsl:call-template name="rights"/-->
            <!--xsl:call-template name="contributor"/-->

        </dc>
    </xsl:template>

    <xsl:template name="title">
        <title xmlns="http://www.openarchives.org/OAI/2.0/oai_dc/">
            <xsl:value-of select="igsn:resourceTitle"/>
        </title>
    </xsl:template>
    <xsl:template name="publisher">
        <publisher xmlns="http://www.openarchives.org/OAI/2.0/oai_dc/">
            <xsl:value-of select="igsn:curatorName"/>
        </publisher>
    </xsl:template>
    <xsl:template name="date">
        <date xmlns="http://www.openarchives.org/OAI/2.0/oai_dc/">
            <xsl:value-of select="."/>
        </date>
    </xsl:template>
    <xsl:template name="description">
        <description xmlns="http://www.openarchives.org/OAI/2.0/oai_dc/">
            <xsl:value-of select="."/>
        </description>
    </xsl:template>
    <xsl:template name="language">
        <language xmlns="http://www.openarchives.org/OAI/2.0/oai_dc/"> </language>
    </xsl:template>
    <xsl:template name="subject">
        <subject xmlns="http://www.openarchives.org/OAI/2.0/oai_dc/">
            <xsl:value-of select="."/>
        </subject>
    </xsl:template>
    <xsl:template name="type">
        <type xmlns="http://www.openarchives.org/OAI/2.0/oai_dc/">
            <xsl:value-of select="."/>
        </type>
    </xsl:template>
    <xsl:template name="identifier">
        <identifier xmlns="http://www.openarchives.org/OAI/2.0/oai_dc/">
            <xsl:value-of select="concat($handle-url, .)"/>
        </identifier>
    </xsl:template>
    <xsl:template name="locality">
        <coverage xmlns="http://www.openarchives.org/OAI/2.0/oai_dc/">
            <xsl:value-of select="."/>
        </coverage>
    </xsl:template>
    <xsl:template name="coverage">
        <coverage xmlns="http://www.openarchives.org/OAI/2.0/oai_dc/">
            <xsl:value-of select="concat($coverage-pre-text, .)"/>
        </coverage>
    </xsl:template>
    <xsl:template name="source">
        <source xmlns="http://www.openarchives.org/OAI/2.0/oai_dc/"> </source>
    </xsl:template>
    <xsl:template name="creator">
        <creator xmlns="http://www.openarchives.org/OAI/2.0/oai_dc/">
            <xsl:value-of select="."/>
        </creator>
    </xsl:template>
    <xsl:template name="relation">
        <relation xmlns="http://www.openarchives.org/OAI/2.0/oai_dc/">
            <xsl:value-of select="."/>
        </relation>
    </xsl:template>
    <xsl:template name="rights">
        <rights xmlns="http://www.openarchives.org/OAI/2.0/oai_dc/"> </rights>
    </xsl:template>
    <xsl:template name="contributor">
        <contributor xmlns="http://www.openarchives.org/OAI/2.0/oai_dc/"> </contributor>
    </xsl:template>
    <xsl:template name="format">
        <format xmlns="http://www.openarchives.org/OAI/2.0/oai_dc/">
            <xsl:value-of select="."/>
        </format>
    </xsl:template>

</xsl:stylesheet>
