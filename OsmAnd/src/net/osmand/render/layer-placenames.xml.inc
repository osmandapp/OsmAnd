<Style name="country">
    <Rule>
      <Filter>[place] = 'country'</Filter>
      &maxscale_zoom2;
      &minscale_zoom3;
      <TextSymbolizer name="name" fontset_name="book-fonts" size="8" fill="#9d6c9d" dy="0" halo_radius="1" wrap_width="20"/>
    </Rule>
    <Rule>
      <Filter>[place] = 'country'</Filter>
      &maxscale_zoom4;
      &minscale_zoom6;
      <TextSymbolizer name="name" fontset_name="book-fonts" size="10" fill="#9d6c9d" dy="0" halo_radius="1" wrap_width="20"/>
    </Rule>
</Style>
<Style name="state">
    <Rule>
      <Filter>[place] = 'state'</Filter>
      &maxscale_zoom4;
      &minscale_zoom4;
      <TextSymbolizer name="ref" fontset_name="oblique-fonts" size="9" fill="#9d6c9d" dy="0" halo_radius="1" wrap_width="0"/>
    </Rule>
    <Rule>
      <Filter>[place] = 'state'</Filter>
      &maxscale_zoom5;
      &minscale_zoom6;
      <TextSymbolizer name="name" fontset_name="oblique-fonts" size="9" fill="#9d6c9d" dy="0" halo_radius="1" wrap_width="0"/>
    </Rule>
    <Rule>
      <Filter>[place] = 'state'</Filter>
      &maxscale_zoom7;
      &minscale_zoom8;
      <TextSymbolizer name="name" fontset_name="oblique-fonts" size="11" fill="#9d6c9d" dy="0" halo_radius="1" wrap_width="0"/>
    </Rule>
</Style>
<Style name="capital">
     <Rule>
      <Filter>[place] = 'city' or [place]='metropolis'</Filter>
      &maxscale_zoom5;
      &minscale_zoom5;
      <TextSymbolizer name="name" fontset_name="book-fonts" size="9" fill="#000" dy="0" halo_radius="1" wrap_width="0"/>
    </Rule>
    <Rule>
      <Filter>[place] = 'city' or [place]='metropolis'</Filter>
      &maxscale_zoom6;
      &minscale_zoom8;
      <TextSymbolizer name="name" fontset_name="book-fonts" size="11" fill="#000" dy="0" halo_radius="1" wrap_width="0"/>
    </Rule>
    <Rule>
      <Filter>[place] = 'city' or [place]='metropolis'</Filter>
      &maxscale_zoom9;
      &minscale_zoom10;
      <TextSymbolizer name="name" fontset_name="book-fonts" size="11" fill="#000" dy="0" halo_radius="1" wrap_width="0"/>
    </Rule>
    <Rule>
      <Filter>[place] = 'city' or [place]='metropolis'</Filter>
      &maxscale_zoom11;
      &minscale_zoom14;
      <TextSymbolizer name="name" fontset_name="book-fonts" size="14" fill="#000" dy="0" halo_radius="1" wrap_width="0"/>
    </Rule>
</Style>
<Style name="city">
    <Rule>
      <Filter>[place] = 'city' or [place]='metropolis'</Filter>
      &maxscale_zoom6;
      &minscale_zoom8;
      <TextSymbolizer name="name" fontset_name="book-fonts" size="8" fill="#000" dy="0" halo_radius="1" wrap_width="0"/>
    </Rule>
    <Rule>
      <Filter>[place] = 'city' or [place]='metropolis'</Filter>
      &maxscale_zoom9;
      &minscale_zoom10;
      <TextSymbolizer name="name" fontset_name="book-fonts" size="11" fill="#000" dy="0" halo_radius="1" wrap_width="0"/>
    </Rule>
    <Rule>
      <Filter>[place] = 'city' or [place]='metropolis'</Filter>
      &maxscale_zoom11;
      &minscale_zoom14;
      <TextSymbolizer name="name" fontset_name="book-fonts" size="14" fill="#000" dy="0" halo_radius="1" wrap_width="0"/>
    </Rule>
</Style>
<Style name="town">
    <Rule>
      <Filter>[place] = 'town' or [place]='large_town' or [place]='small_town'</Filter>
      &maxscale_zoom9;
      &minscale_zoom10;
      <TextSymbolizer name="name" fontset_name="book-fonts" size="8" fill="#000" halo_radius="1" wrap_width="20"/>
    </Rule>
    <Rule>
      <Filter>[place] = 'town' or [place]='large_town' or [place]='small_town'</Filter>
      &maxscale_zoom11;
      &minscale_zoom13;
      <TextSymbolizer name="name" fontset_name="book-fonts" size="10" fill="#000" halo_radius="1" wrap_width="20"/>
    </Rule>
    <Rule>
      <Filter>[place] = 'town' or [place]='large_town' or [place]='small_town'</Filter>
      &maxscale_zoom14;
      <TextSymbolizer name="name" fontset_name="book-fonts" size="14" fill="#777777" halo_radius="1" wrap_width="20"/>
    </Rule>
</Style>
<Style name="suburb">
    <Rule>
      <Filter>[place] = 'suburb'</Filter>
      &maxscale_zoom12;
      &minscale_zoom13;
      <TextSymbolizer name="name" fontset_name="book-fonts" size="10" fill="#000" halo_radius="1" wrap_width="0"/>
    </Rule>
    <Rule>
      <Filter>[place] = 'suburb'</Filter>
      &maxscale_zoom14;
      <TextSymbolizer name="name" fontset_name="book-fonts" size="13" fill="#777777" halo_radius="1" wrap_width="0"/>
    </Rule>
</Style>
<Style name="village">
    <Rule>
      <Filter>[place] = 'village' or [place]='large_village'</Filter>
      &maxscale_zoom12;
      &minscale_zoom14;
      <TextSymbolizer name="name" fontset_name="book-fonts" size="9" fill="#000" halo_radius="1" wrap_width="0"/>
    </Rule>
    <Rule>
      <Filter>[place] = 'village' or [place]='large_village'</Filter>
      &maxscale_zoom15;
      <TextSymbolizer name="name" fontset_name="book-fonts" size="12" fill="#777777" halo_radius="1" wrap_width="0"/>
    </Rule>
</Style>
<Style name="hamlet">
    <Rule>
      <Filter>[place] = 'hamlet' or [place] = 'locality'</Filter>
      &maxscale_zoom14;
      &minscale_zoom15;
      <TextSymbolizer name="name" fontset_name="book-fonts" size="8" fill="#000" halo_radius="1" wrap_width="0"/>
    </Rule>
    <Rule>
      <Filter>[place] = 'hamlet' or [place] = 'locality'</Filter>
      &maxscale_zoom16;
      <TextSymbolizer name="name" fontset_name="book-fonts" size="11" fill="#777777" halo_radius="1" wrap_width="0"/>
    </Rule>
</Style>

<Layer name="placenames-large" status="on" srs="&osm2pgsql_projection;">
    <StyleName>country</StyleName>
    <StyleName>state</StyleName>
    <StyleName>capital</StyleName>
    <Datasource>
      <Parameter name="table">
      (select way,place,name,ref
      from &prefix;_point
      where place in ('country','state')
         or (place in ('city','metropolis') and capital='yes')
      ) as placenames</Parameter>
      &datasource-settings;
    </Datasource>
</Layer>
<Layer name="placenames-medium" status="on" srs="&osm2pgsql_projection;">
    <StyleName>city</StyleName>
    <StyleName>town</StyleName>
    <Datasource>
      <Parameter name="table">
      (select way,place,name
      from &prefix;_point
      where place in ('city','metropolis','town','large_town','small_town')
        and (capital is null or capital&lt;&gt;'yes')
      ) as placenames</Parameter>
      &datasource-settings;
    </Datasource>
</Layer>
<Layer name="placenames-small" status="on" srs="&osm2pgsql_projection;">
    <StyleName>suburb</StyleName>
    <StyleName>village</StyleName>
    <StyleName>hamlet</StyleName>
    <Datasource>
      <Parameter name="table">
      (select way,place,name
      from &prefix;_point
      where place in ('suburb','village','large_village','hamlet','locality')
      ) as placenames</Parameter>
      &datasource-settings;
    </Datasource>
</Layer>

