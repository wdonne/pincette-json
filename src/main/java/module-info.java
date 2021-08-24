module net.pincette.json {
  requires java.json;
  requires java.xml;
  requires net.pincette.common;
  requires com.fasterxml.jackson.databind;
  requires jslt;
  requires java.logging;
  requires com.fasterxml.jackson.dataformat.yaml;
  exports net.pincette.json;
  exports net.pincette.json.filter;
}
