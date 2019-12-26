package com.onkiup.linker.evaluator.sail.context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.onkiup.linker.evaluator.common.AbstractContext;
import com.onkiup.linker.grammar.sail.token.LisaValueToken;
import com.onkiup.linker.parser.Rule;
import com.onkiup.linker.parser.TokenGrammar;

public class AppianFileSystemContext extends AbstractContext<String> {

  private static final Logger logger = LoggerFactory.getLogger(AppianFileSystemContext.class);
  private static AppianFileSystemContext INSTANCE;

  private static final DocumentBuilder documentBuilder;
  private static final XPath xpath = XPathFactory.newInstance().newXPath();
  private static final XPathExpression bodyQuery;
  private static final XPathExpression paramQuery;
  private static final XPathExpression paramNameQuery;
  private static final XPathExpression paramTypeQuery;
  private static final HashMap<String, Class> appianTypeMap = new HashMap<>();

  static {
    try {
      documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      bodyQuery = xpath.compile("//rule/definition/text()");
      paramQuery = xpath.compile("//rule/namedTypedValue");
      paramNameQuery = xpath.compile("/name/text()");
      paramTypeQuery = xpath.compile("/type/name/text()");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private File root;
  private ConcurrentHashMap<String, Object> members = new ConcurrentHashMap<>();

  public AppianFileSystemContext(File root) throws IOException {
    super(null, null);
    INSTANCE = this;
    this.root = root;
    long loaded = Files.walk(root.toPath())
        .filter(path -> {
          String fileName = path.getFileName().toString();
          return fileName.startsWith("SYSTEM_SYSRULES_") && fileName.endsWith(".xml");
        })
        .peek(path -> {
          String fileName = path.getFileName().toString();
          members.put(fileName.substring(16, fileName.length() - 4).toLowerCase(), path);
        }).count();
    logger.info("Pre-loaded references to " + loaded + " appian rules from '" + root + "'");
  }

  public static AppianFileSystemContext instance() {
    return INSTANCE;
  }

  @Override
  public Optional<?> resolveLocally(String key) {
    String[] elements = key.split("!");
    if (elements.length == 2 && !elements[0].equals("a")) {
      // only "a!" domain is supported :)
      return Optional.empty();
    }
    logger.info("Resolving rule {}", key);
    String name = (elements.length == 2 ? elements[1] : elements[0]).toLowerCase();
    if (!members.containsKey(name)) {
      return Optional.empty();
    }
    Object result = members.get(name);
    if (result instanceof Path) {
      try {
        result = readSysRule((Path)result);
        members.put(name, result);
      } catch (Exception e) {
        throw new IllegalStateException("Failed to load Appian system rule '" + name + "' from file '" + result + "'", e);
      }
    }
    return Optional.ofNullable(result);
  }

  @Override
  public void store(String key, Object value, boolean modifiable, boolean override) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String,Object> members() {
    return members;
  }

  @Override
  public void close() {

  }

  private LisaValueToken readSysRule(Path path) {
    return cached(path).orElseGet(() -> {
      try {
        Document document = documentBuilder.parse(path.toFile());
        document.getDocumentElement().normalize();
        Node body = (Node)bodyQuery.evaluate(document, XPathConstants.NODE);
        LisaValueToken result = TokenGrammar.forClass(LisaValueToken.class).parse(path.toString(), body.getNodeValue());

        NodeList parameterNodes = (NodeList)paramQuery.evaluate(document, XPathConstants.NODESET);
        Map<String,Class> parameters = new HashMap<>();
        for (int i = 0; i < parameterNodes.getLength(); i++) {
          Node namedTypedValueNode = parameterNodes.item(i);
          String paramName = (String)paramNameQuery.evaluate(namedTypedValueNode, XPathConstants.STRING);
          String paramTypeName = (String)paramTypeQuery.evaluate(namedTypedValueNode, XPathConstants.STRING);
          Class paramType = appianTypeMap.getOrDefault(paramTypeName, Object.class);
          parameters.put(paramName, paramType);
        }

        cache(path, result);
        return result;
      } catch (Exception e) {
        // TODO: introduce a separate exception type
        throw new RuntimeException(e);
      }
    });
  }

  private String hash(Path file) throws NoSuchAlgorithmException, IOException {
    String code = IOUtils.toString(new FileReader(file.toFile()));
    return hash(code);
  }

  private String hash(CharSequence text) throws NoSuchAlgorithmException {
    StringBuilder result = new StringBuilder();
    byte[] bytes = MessageDigest.getInstance("SHA-256").digest(text.toString().getBytes());
    for (byte b : bytes) {
      result.append(String.format("%02x", b & 0xff));
    }
    return result.toString();
  }

  private Optional<LisaValueToken> cached(Path path) {
    try {
      String name = path.getFileName().toString();
      String codehash = hash(path);
      File temp = File.createTempFile(name, ".lisa_cache");
      temp.delete();
      File source = new File(temp.getParentFile(), name + ".lisa_cache");
      if (source.exists()) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(source))) {
          String filehash = (String)ois.readObject();
          if (Objects.equals(filehash, codehash)) {
            LisaValueToken result = Rule.load(ois);
            return Optional.of(result);
          } else {
            source.delete();
          }
        } catch (Exception e) {
          source.delete();
        }
      }
    } catch (Exception e) {
      logger.warn("Failed to load cache for path '{}': {}", path, e.getLocalizedMessage());
    }
    return Optional.empty();
  }

  private void cache(Path source, LisaValueToken value) {
    try {
      String name = source.getFileName().toString();
      String hash = hash(source);
      File temp = File.createTempFile(name, ".lisa_cache");
      temp.delete();
      File target = new File(temp.getParentFile(), name + ".lisa_cache");
      try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(target))){
        oos.writeObject(hash);
        value.store(oos);
      }
      logger.debug("Cached sail tree of '{}' as '{}'", source, target);
    } catch (Exception e) {
      logger.warn("Failed to store cache for path '{}': {}", source, e.getMessage());
    }
  }
}
