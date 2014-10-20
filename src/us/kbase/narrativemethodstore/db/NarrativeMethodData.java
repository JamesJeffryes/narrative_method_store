package us.kbase.narrativemethodstore.db;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.JsonNode;

import us.kbase.common.service.UObject;
import us.kbase.narrativemethodstore.CheckboxOptions;
import us.kbase.narrativemethodstore.DropdownOptions;
import us.kbase.narrativemethodstore.FloatSliderOptions;
import us.kbase.narrativemethodstore.IntSliderOptions;
import us.kbase.narrativemethodstore.MethodBehavior;
import us.kbase.narrativemethodstore.MethodBriefInfo;
import us.kbase.narrativemethodstore.MethodFullInfo;
import us.kbase.narrativemethodstore.MethodParameter;
import us.kbase.narrativemethodstore.MethodSpec;
import us.kbase.narrativemethodstore.RadioOptions;
import us.kbase.narrativemethodstore.ScreenShot;
import us.kbase.narrativemethodstore.ServiceMethodInputMapping;
import us.kbase.narrativemethodstore.ServiceMethodOutputMapping;
import us.kbase.narrativemethodstore.TextAreaOptions;
import us.kbase.narrativemethodstore.TextOptions;
import us.kbase.narrativemethodstore.WidgetSpec;
import us.kbase.narrativemethodstore.exceptions.NarrativeMethodStoreException;

public class NarrativeMethodData {
	protected String methodId;
	protected MethodBriefInfo briefInfo;
	protected MethodFullInfo fullInfo;
	protected MethodSpec methodSpec;
	
	public NarrativeMethodData(String methodId, JsonNode spec, Map<String, Object> display,
			MethodFileLookup lookup) throws NarrativeMethodStoreException {
		try {
			update(methodId, spec, display, lookup);
		} catch (Throwable ex) {
			if (briefInfo.getName() == null)
				briefInfo.withName(briefInfo.getId());
			if (briefInfo.getCategories() == null)
				briefInfo.withCategories(Arrays.asList("error"));
				NarrativeMethodStoreException ret = new NarrativeMethodStoreException(ex.getMessage(), ex);
				briefInfo.setLoadingError(ex.getMessage());
				ret.setErrorMethod(briefInfo);
		}
	}
	
	public MethodBriefInfo getMethodBriefInfo() {
		return briefInfo;
	}
	
	public MethodFullInfo getMethodFullInfo() {
		return fullInfo;
	}
	
	public MethodSpec getMethodSpec() {
		return methodSpec;
	}
	
	
	public void update(String methodId, JsonNode spec, Map<String, Object> display,
			MethodFileLookup lookup) throws NarrativeMethodStoreException {
		this.methodId = methodId;

		briefInfo = new MethodBriefInfo()
							.withId(this.methodId);

		List <String> categories = new ArrayList<String>(1);
		JsonNode cats = get(spec, "categories");
		for(int k=0; k<cats.size(); k++) {
			categories.add(cats.get(k).asText());
		}
		briefInfo.withCategories(categories);
		
		String methodName = getDisplayProp(display, "name", lookup);
		briefInfo.withName(methodName);
		String methodSubtitle = getDisplayProp(display, "subtitle", lookup);
		briefInfo.withSubtitle(methodSubtitle);
		String methodTooltip = getDisplayProp(display, "tooltip", lookup);
		briefInfo.withTooltip(methodTooltip);
		String methodDescription = getDisplayProp(display, "description", lookup);
		String methodTechnicalDescr = getDisplayProp(display, "technical-description", lookup);
		
		briefInfo.withVer(get(spec, "ver").asText());
		
		List <String> authors = new ArrayList<String>(2);
		for(int a=0; a< get(spec, "authors").size(); a++) {
			authors.add(get(spec, "authors").get(a).asText());
		}
		
		List<ScreenShot> screenshots = new ArrayList<ScreenShot>();
		@SuppressWarnings("unchecked")
		List<String> imageNames = (List<String>)getDisplayProp("/", display, "screenshots");
		if (imageNames != null) {
			for (String imageName : imageNames)
				screenshots.add(new ScreenShot().withUrl("img?method_id=" + this.methodId + "&image_name=" + imageName));
		}
		
		fullInfo = new MethodFullInfo()
							.withId(this.methodId)
							.withName(methodName)
							.withVer(briefInfo.getVer())
							.withSubtitle(methodSubtitle)
							.withTooltip(methodTooltip)
							.withCategories(categories)
							
							.withAuthors(null)
							.withContact(get(spec, "contact").asText())
							
							.withDescription(methodDescription)
							.withTechnicalDescription(methodTechnicalDescr)
							.withScreenshots(screenshots);
		
		JsonNode widgetsNode = get(spec, "widgets");
		WidgetSpec widgets = new WidgetSpec()
							.withInput(getTextOrNull(widgetsNode.get("input")))
							.withOutput(getTextOrNull(widgetsNode.get("output")));
		JsonNode behaviorNode = get(spec, "behavior");
		JsonNode serviceMappingNode = behaviorNode.get("service-mapping");
		MethodBehavior behavior = new MethodBehavior()
							.withPythonClass(getTextOrNull(behaviorNode.get("python_class")))
							.withPythonFunction(getTextOrNull(behaviorNode.get("python_function")));
		if (serviceMappingNode != null) {
			JsonNode paramsMappingNode = get("behavior/service-mapping", serviceMappingNode, "input_mapping");
			List<ServiceMethodInputMapping> paramsMapping = new ArrayList<ServiceMethodInputMapping>();
			for (int j = 0; j < paramsMappingNode.size(); j++) {
				JsonNode paramMappingNode = paramsMappingNode.get(j);
				String path = "behavior/service-mapping/input_mapping/" + j;
				ServiceMethodInputMapping paramMapping = new ServiceMethodInputMapping();
				for (Iterator<String> it2 = paramMappingNode.fieldNames(); it2.hasNext(); ) {
					String field = it2.next();
					if (field.equals("target_argument_position")) {
						paramMapping.withTargetArgumentPosition(getLongOrNull(paramMappingNode.get(field)));
					} else if (field.equals("target_property")) {
						paramMapping.withTargetProperty(getTextOrNull(paramMappingNode.get(field)));
					} else if (field.equals("target_type_transform")) {
						paramMapping.withTargetTypeTransform(getTextOrNull(paramMappingNode.get(field)));
					} else if (field.equals("input_parameter")) {
						paramMapping.withInputParameter(paramMappingNode.get(field).asText());
					} else if (field.equals("narrative_system_variable")) {
						paramMapping.withNarrativeSystemVariable(paramMappingNode.get(field).asText());
					} else if (field.equals("constant_value")) {
						paramMapping.withConstantValue(new UObject(paramMappingNode.get(field)));
					} else {
						throw new IllegalStateException("Unknown field [" + field + "] in method parameter " +
								"mapping structure within path " + path);
					}
				}
				paramsMapping.add(paramMapping);
			}
			List<ServiceMethodOutputMapping> outputMapping = new ArrayList<ServiceMethodOutputMapping>();
			JsonNode outputMappingNode = get("behavior/service-mapping", serviceMappingNode, "output_mapping");
			for (int j = 0; j < outputMappingNode.size(); j++) {
				JsonNode paramMappingNode = outputMappingNode.get(j);
				String path = "behavior/service-mapping/output_mapping/" + j;
				ServiceMethodOutputMapping paramMapping = new ServiceMethodOutputMapping();
				for (Iterator<String> it2 = paramMappingNode.fieldNames(); it2.hasNext(); ) {
					String field = it2.next();
					if (field.equals("target_property")) {
						paramMapping.withTargetProperty(getTextOrNull(paramMappingNode.get(field)));
					} else if (field.equals("target_type_transform")) {
						paramMapping.withTargetTypeTransform(getTextOrNull(paramMappingNode.get(field)));
					} else if (field.equals("input_parameter")) {
						paramMapping.withInputParameter(paramMappingNode.get(field).asText());
					} else if (field.equals("narrative_system_variable")) {
						paramMapping.withNarrativeSystemVariable(paramMappingNode.get(field).asText());
					} else if (field.equals("constant_value")) {
						paramMapping.withConstantValue(new UObject(paramMappingNode.get(field)));
					} else if (field.equals("service_method_output_path")) {
						paramMapping.withServiceMethodOutputPath(jsonListToStringList(paramMappingNode.get(field)));
					} else {
						throw new IllegalStateException("Unknown field [" + field + "] in method output " +
								"mapping structure within path " + path);
					}
				}
				outputMapping.add(paramMapping);
			}
			behavior
				.withKbServiceUrl(getTextOrNull(get("behavior/service-mapping", serviceMappingNode, "url")))
				.withKbServiceName(getTextOrNull(serviceMappingNode.get("name")))
				.withKbServiceMethod(getTextOrNull(get("behavior/service-mapping", serviceMappingNode, "method")))
				.withKbServiceInputMapping(paramsMapping)
				.withKbServiceOutputMapping(outputMapping);
		}
		List<MethodParameter> parameters = new ArrayList<MethodParameter>();
		JsonNode parametersNode = get(spec, "parameters");
		@SuppressWarnings("unchecked")
		Map<String, Object> paramsDisplays = (Map<String, Object>)getDisplayProp("/", display, "parameters");
		Set<String> paramIds = new TreeSet<String>();
		for (int i = 0; i < parametersNode.size(); i++) {
			JsonNode paramNode = parametersNode.get(i);
			String paramPath = "parameters/" + i;
			String paramId = get(paramPath, paramNode, "id").asText();
			paramIds.add(paramId);
			@SuppressWarnings("unchecked")
			Map<String, Object> paramDisplay = (Map<String, Object>)getDisplayProp("parameters", paramsDisplays, paramId);
			TextOptions textOpt = null;
			if (paramNode.has("text_options")) {
				JsonNode optNode = get(paramPath, paramNode, "text_options");
				textOpt = new TextOptions()
							.withValidWsTypes(jsonListToStringList(optNode.get("valid_ws_types")))
							.withValidateAs(getTextOrNull(optNode.get("validate_as")));
			}
			CheckboxOptions cbOpt = null;
			if (paramNode.has("checkbox_options")) {
				JsonNode optNode = get(paramPath, paramNode, "checkbox_options");
				long checkedValue = get(paramPath + "/checkbox_options", optNode, "checked_value").asLong();
				long uncheckedValue = get(paramPath + "/checkbox_options", optNode, "unchecked_value").asLong();
				cbOpt = new CheckboxOptions().withCheckedValue(checkedValue).withUncheckedValue(uncheckedValue);
			}
			DropdownOptions ddOpt = null;
			if (paramNode.has("dropdown_options")) {
				JsonNode optNode = get(paramPath, paramNode, "dropdown_options");
				optNode = get(paramPath + "/dropdown_options", optNode, "options");
				Map<String, String> options = new LinkedHashMap<String, String>();
				for (int j = 0; j < optNode.size(); j++) {
					JsonNode itemNode = optNode.get(j);
					String id = get(paramPath + "/dropdown_options/" + j, itemNode, "id").asText();
					String uiName = get(paramPath + "/dropdown_options/" + j, itemNode, "ui_name").asText();
					options.put(id, uiName);
				}
				ddOpt = new DropdownOptions().withIdsToOptions(options);
			}
			FloatSliderOptions floatOpt = null;
			if (paramNode.has("floatslider_options")) {
				JsonNode optNode = get(paramPath, paramNode, "floatslider_options");
				double min = get(paramPath + "/floatslider_options", optNode, "min").asDouble();
				double max = get(paramPath + "/floatslider_options", optNode, "max").asDouble();
				floatOpt = new FloatSliderOptions().withMin(min).withMax(max);
			}
			IntSliderOptions intOpt = null;
			if (paramNode.has("intslider_options")) {
				JsonNode optNode = get(paramPath, paramNode, "intslider_options");
				long min = get(paramPath + "/intslider_options", optNode, "min").asLong();
				long max = get(paramPath + "/intslider_options", optNode, "max").asLong();
				long step = get(paramPath + "/intslider_options", optNode, "step").asLong();
				intOpt = new IntSliderOptions().withMin(min).withMax(max).withStep(step);
			}
			RadioOptions radioOpt = null;
			if (paramNode.has("radio_options")) {
				JsonNode optNode = get(paramPath, paramNode, "radio_options");
				optNode = get(paramPath + "/radio_options", optNode, "options");
				Map<String, String> options = new LinkedHashMap<String, String>();
				Map<String, String> tooltips = new LinkedHashMap<String, String>();
				for (int j = 0; j < optNode.size(); j++) {
					JsonNode itemNode = optNode.get(j);
					String id = get(paramPath + "/radio_options/" + j, itemNode, "id").asText();
					String uiName = get(paramPath + "/radio_options/" + j, itemNode, "ui_name").asText();
					String uiTooltip = get(paramPath + "/radio_options/" + j, itemNode, "ui_tooltip").asText();
					options.put(id, uiName);
					tooltips.put(id, uiTooltip);
				}
				radioOpt = new RadioOptions().withIdsToOptions(options).withIdsToTooltip(tooltips);
			}
			TextAreaOptions taOpt = null;
			if (paramNode.has("textarea_options")) {
				JsonNode optNode = get(paramPath, paramNode, "textarea_options");
				long nRows = get(paramPath + "/textarea_options", optNode, "n_rows").asLong();
				taOpt = new TextAreaOptions().withNRows(nRows);
			}
			MethodParameter param = new MethodParameter()
							.withId(paramId)
							.withUiName((String)getDisplayProp("parameters/" + paramId, paramDisplay, "ui-name"))
							.withShortHint((String)getDisplayProp("parameters/" + paramId, paramDisplay, "short-hint"))
							.withLongHint((String)getDisplayProp("parameters/" + paramId, paramDisplay, "long-hint"))
							.withOptional(jsonBooleanToRPC(get(paramPath, paramNode, "optional")))
							.withAdvanced(jsonBooleanToRPC(get(paramPath, paramNode, "advanced")))
							.withAllowMultiple(jsonBooleanToRPC(get(paramPath, paramNode, "allow_multiple")))
							.withDefaultValues(jsonListToStringList(get(paramPath, paramNode, "default_values")))
							.withFieldType(get(paramPath, paramNode, "field_type").asText())
							.withTextOptions(textOpt)
							.withCheckboxOptions(cbOpt)
							.withDropdownOptions(ddOpt)
							.withFloatsliderOptions(floatOpt)
							.withIntsliderOptions(intOpt)
							.withRadioOptions(radioOpt)
							.withTextareaOptions(taOpt);
			parameters.add(param);
		}
		if (behavior.getKbServiceInputMapping() != null) {
			for (int i = 0; i < behavior.getKbServiceInputMapping().size(); i++) {
				ServiceMethodInputMapping mapping = behavior.getKbServiceInputMapping().get(i);
				String paramId = mapping.getInputParameter();
				if (paramId != null && !paramIds.contains(paramId)) {
					throw new IllegalStateException("Undeclared parameter [" + paramId + "] found " +
							"within path [behavior/service-mapping/input_mapping/" + i + "]");
				}
			}
		}
		if (behavior.getKbServiceOutputMapping() != null) {
			for (int i = 0; i < behavior.getKbServiceOutputMapping().size(); i++) {
				ServiceMethodOutputMapping mapping = behavior.getKbServiceOutputMapping().get(i);
				String paramId = mapping.getInputParameter();
				if (paramId != null && !paramIds.contains(paramId)) {
					throw new IllegalStateException("Undeclared parameter [" + paramId + "] found " +
							"within path [behavior/service-mapping/output_mapping/" + i + "]");
				}
			}
		}
		methodSpec = new MethodSpec()
							.withInfo(briefInfo)
							.withWidgets(widgets)
							.withBehavior(behavior)
							.withParameters(parameters);
	}

	private static JsonNode get(JsonNode node, String childName) {
		return get(null, node, childName);
	}
	
	private static JsonNode get(String nodePath, JsonNode node, String childName) {
		JsonNode ret = node.get(childName);
		if (ret == null)
			throw new IllegalStateException("Can't find sub-node [" + childName + "] within " +
					"path [" + (nodePath == null ? "/" : nodePath) + "] in spec.json");
		return ret;
	}
	
	private static String getDisplayProp(Map<String, Object> display, String propName, 
			MethodFileLookup lookup) {
		return getDisplayProp(display, propName, lookup, propName);
	}

	private static String getDisplayProp(Map<String, Object> display, String propName, 
			MethodFileLookup lookup, String lookupName) {
		String ret = lookup.loadFileContent(lookupName + ".html");
		if (ret == null)
			ret = (String)getDisplayProp("/", display, propName);
		return ret;
	}
	
	private static Object getDisplayProp(String path, Map<String, Object> display, String propName) {
		Object ret = display.get(propName);
		if (ret == null)
			throw new IllegalStateException("Can't find property [" + propName + "] within path [" + 
					path + "] in display.yaml");
		return ret;
	}

	private static String getTextOrNull(JsonNode node) {
		return node == null ? null : node.asText();
	}

	private static Long getLongOrNull(JsonNode node) {
		return node == null || node.isNull() ? null : node.asLong();
	}

	private static Long jsonBooleanToRPC(JsonNode node) {
		return node.asBoolean() ? 1L : 0L;
	}
	
	private static List<String> jsonListToStringList(JsonNode node) {
		if (node == null)
			return null;
		List<String> ret = new ArrayList<String>();
		for (int i = 0; i < node.size(); i++)
			ret.add(node.get(i).asText());
		return ret;
	}

	private static Map<String, String> jsonMapToStringMap(JsonNode node) {
		if (node == null)
			return null;
		Map<String, String> ret = new LinkedHashMap<String, String>();
		for (Iterator<String> it = node.fieldNames(); it.hasNext(); ) {
			String key = it.next();
			ret.put(key, node.get(key).asText());
		}
		return ret;
	}
}
