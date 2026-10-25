-- Fictional local demo only. Never apply to an existing business database.
-- Demo administrator password is 123456 (legacy MD5 compatibility, NOT a deployment password).
INSERT IGNORE INTO employee (id,name,username,password,phone,sex,id_number,status,create_time,update_time)
VALUES (1,'演示管理员','admin','e10adc3949ba59abbe56e057f20f883e','13900000000','1','000000000000000000',1,NOW(),NOW());
INSERT IGNORE INTO category (id,type,name,sort,status,create_time,update_time,create_user,update_user)
VALUES (1,1,'演示生鲜',1,1,NOW(),NOW(),1,1);
INSERT IGNORE INTO dish (id,name,category_id,price,image,description,status,is_groupon,groupon_price,groupon_size,groupon_valid_hours,stock,unit,origin,is_fresh,create_time,update_time,create_user,update_user)
VALUES (1,'演示苹果',1,12.50,'/static/images/png/dish-1.png','虚构测试商品，所有价格与库存均为演示数据',1,1,10.00,2,24,99,'500g/份','演示果园',1,NOW(),NOW(),1,1),
       (2,'演示青菜',1,6.00,'/static/images/png/dish-2.png','虚构测试商品',1,0,NULL,2,24,100,'500g/份','演示农场',1,NOW(),NOW(),1,1);
INSERT IGNORE INTO pickup_point (id,name,community,address,contact,contact_name,phone,contact_phone,latitude,longitude,image,service_tags,open_time,status,create_time,update_time)
VALUES (1,'演示社区自提点','演示社区','虚构测试路1号','演示人员','演示人员','13900000000','13900000000',31.2304000,121.4737000,'/static/images/png/dish-1.png','支持冷藏','每日08:00-20:00',1,NOW(),NOW());
INSERT IGNORE INTO user (id,openid,name,phone,password,create_time)
VALUES (1,'fictional_fixture_one','虚构历史用户一','13900000001',NULL,'1999-12-31 10:00:00'),
       (2,'fictional_fixture_two','虚构历史用户二','13900000002',NULL,'1999-12-31 10:00:00');
-- Independent A12 worked example: 3 units x 12.50 = 37.50, completed and paid.
INSERT IGNORE INTO orders (id,number,status,user_id,order_time,checkout_time,pay_method,pay_status,amount,phone,address,consignee,delivery_status,tableware_status,is_groupon,verify_status)
VALUES (1,'FIXTURE-20000101-001',5,1,'2000-01-01 12:00:00','2000-01-01 12:01:00',1,1,37.50,'13900000001','虚构历史地址','虚构历史用户一',1,1,0,0);
INSERT IGNORE INTO order_detail (id,name,order_id,dish_id,number,amount)
VALUES (1,'演示苹果',1,1,3,12.50);
-- One reserved unit in an expired, unclosed group; A05 must refund and restore once.
INSERT IGNORE INTO groupon (id,dish_id,leader_user_id,groupon_price,required_size,current_size,status,expire_time,pickup_point_id,create_time,update_time)
VALUES (1,1,1,10.00,2,1,1,'2001-01-01 13:00:00',1,'2001-01-01 12:00:00','2001-01-01 12:00:00');
INSERT IGNORE INTO orders (id,number,status,user_id,order_time,checkout_time,pay_method,pay_status,amount,phone,consignee,pickup_point_id,pickup_point_name,pickup_person_name,pickup_person_phone,is_groupon,groupon_id,verify_status)
VALUES (2,'FIXTURE-EXPIRED-001',2,1,'2001-01-01 12:00:00','2001-01-01 12:01:00',1,1,10.00,'13900000001','虚构历史用户一',1,'演示社区自提点','虚构历史用户一','13900000001',1,1,0);
INSERT IGNORE INTO order_detail (id,name,order_id,dish_id,number,amount)
VALUES (2,'演示苹果',2,1,1,10.00);
INSERT IGNORE INTO groupon_member (id,groupon_id,user_id,order_id,is_leader,number,amount,create_time)
VALUES (1,1,1,2,1,1,10.00,'2001-01-01 12:00:00');
INSERT IGNORE INTO order_stock (order_id,dish_id,number) VALUES (2,1,1);
