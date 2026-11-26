INSERT INTO `tb_department` (`code`, `name`, `status`)
VALUES
  ('CS', '计算机与信息技术学院', 1),
  ('EE', '电子信息工程学院', 1);

INSERT INTO `tb_fitness_class` (`department_id`, `name`, `grade_year`, `teacher_id`)
SELECT `id`, '计算机科学与技术1班', 2026, NULL FROM `tb_department` WHERE `code` = 'CS'
UNION ALL
SELECT `id`, '软件工程1班', 2026, NULL FROM `tb_department` WHERE `code` = 'CS'
UNION ALL
SELECT `id`, '电子信息工程1班', 2026, NULL FROM `tb_department` WHERE `code` = 'EE'
UNION ALL
SELECT `id`, '通信工程1班', 2026, NULL FROM `tb_department` WHERE `code` = 'EE';
