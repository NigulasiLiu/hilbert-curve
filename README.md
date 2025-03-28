# DSSE-RSKQ: 基于动态可搜索加密的空间关键词查询方案

[![CI](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/NigulasiLiu/hilbert-curve/actions) 
[![Java](https://img.shields.io/badge/Java-17%2B-orange)](https://openjdk.org/) 
[![DOI](https://img.shields.io/badge/DOI-10.xxxx/xxxxxx-blue)](https://doi.org/xx.xxxx) 
[![License](https://img.shields.io/badge/License-Apache%202.0-yellow)](https://opensource.org/licenses/Apache-2.0)

> 📌 **核心创新**：通过希尔伯特曲线双位图索引解决传统DSSE方案在8%×8%查询区域下10.78%的错误率问题

## 目录
- [特性亮点](#特性亮点)
- [快速开始](#快速开始)
- [算法原理](#算法原理)
- [性能对比](#性能对比)
- [引用方式](#引用方式)

## 特性亮点
| 特性           | 技术实现                  | 优势                 |
| -------------- | ------------------------- | -------------------- |
| **搜索正确性** | 希尔伯特前缀编码          | 错误率降至0%         |
| **查询效率**   | 双位图索引结构            | 搜索速度提升3.45×    |
| **动态更新**   | 平衡树优化                | 更新速度提升1.895×   |
| **多查询支持** | 几何/布尔范围查询分离处理 | 同时支持GRQ和BRQ查询 |

## 
