[![](https://travis-ci.com/imagej/imagej-modelzoo.svg?branch=master)](https://travis-ci.com/imagej/imagej-modelzoo)

# imagej-modelzoo

This is an ImageJ consumer for models from the [bioimage model zoo](https://github.com/bioimage-io/bioimage-model-zoo).

Current idea of a yaml file which matches the model zoo format but includes Java references:

```
name: denoise2d
description: Dummy denoise example
format_version: 0.1.0
language: java
framework: maven
inputs:
  - name: input
    axes: byxc
    data_type: float32
    data_range: [-inf, inf]
    shape:
      min: [1, 4, 4, 1]
      step: [null, 4, 4, 0]
outputs:
  - name: output
    axes: byxc
    data_type: float32
    data_range: [-inf, inf]
    shape:
      reference_input: input
      scale: [1, 1, 1, 1]
      offset: [0, 0, 0, 0]
      halo: [0, 32, 32, 0]

prediction:
  preprocess:
    - spec: net.imagej.modelzoo.transform.normalize.PercentileNormalizer
      kwargs:
        {
          data: [0],
          min: 0.3,
          max: 0.98
        }
  weights:
    source: ./
  dependencies: maven:../../../../pom.xml
```
