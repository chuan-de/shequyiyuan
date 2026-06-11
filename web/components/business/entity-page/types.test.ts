import { describe, expect, it } from 'vitest';
import { initForm, isoToLocalInput, type EntityFormField } from './types';

describe('isoToLocalInput', () => {
  it('输出 <input type="datetime-local"> 形状（yyyy-MM-ddTHH:mm）', () => {
    expect(isoToLocalInput('2026-06-11T08:30:00Z')).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/);
  });

  it('换算为本地时区且分钟级往返一致', () => {
    const iso = '2026-06-11T08:30:00Z';
    const local = isoToLocalInput(iso);
    // datetime-local 字符串按本地时区解释，应还原出同一时刻
    expect(new Date(local).getTime()).toBe(new Date(iso).getTime());
  });

  it('非法输入返回空串', () => {
    expect(isoToLocalInput('not-a-date')).toBe('');
    expect(isoToLocalInput('')).toBe('');
  });
});

describe('initForm', () => {
  const fields: EntityFormField[] = [
    { key: 'name', label: '名称', defaultValue: '默认名' },
    { key: 'amount', label: '数量' },
    { key: 'measuredAt', label: '测量时间', type: 'datetime' },
    { key: 'birthDate', label: '出生日期', type: 'date' },
    { key: 'extra', label: '附加', type: 'custom' },
  ];

  it('新建（无 row）时取 defaultValue，datetime 预填当前时间', () => {
    const form = initForm(fields);
    expect(form.name).toBe('默认名');
    expect(form.amount).toBe('');
    expect(form.measuredAt).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/);
    expect(form.birthDate).toBe('');
  });

  it('编辑时字符串字段原样、数字字段转字符串', () => {
    const form = initForm(fields, { id: 1, name: '复诊', amount: 42 });
    expect(form.name).toBe('复诊');
    expect(form.amount).toBe('42');
  });

  it('编辑时 datetime 字段从 ISO 转为本地 input 格式，缺失则为空', () => {
    const withValue = initForm(fields, { id: 1, measuredAt: '2026-06-11T08:30:00Z' });
    expect(new Date(withValue.measuredAt).getTime()).toBe(new Date('2026-06-11T08:30:00Z').getTime());

    const without = initForm(fields, { id: 1 });
    expect(without.measuredAt).toBe('');
  });

  it('date 字段截取 yyyy-MM-dd（适配 <input type="date">）', () => {
    const form = initForm(fields, { id: 1, birthDate: '1990-05-20T00:00:00Z' });
    expect(form.birthDate).toBe('1990-05-20');
  });

  it('custom 字段：非字符串值 JSON 序列化保存', () => {
    const form = initForm(fields, { id: 1, extra: { a: 1 } as unknown as string });
    expect(form.extra).toBe('{"a":1}');
  });
});
