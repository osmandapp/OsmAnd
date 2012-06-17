projects := protobuf zlib png skia expat freetype osmand

.DEFAULT: installr
.PHONY: release debug clean installr installd

installr release debug clean installd:
	@- $(foreach project,$(projects), \
		$(MAKE) --directory=./$(project) $@; \
	)
