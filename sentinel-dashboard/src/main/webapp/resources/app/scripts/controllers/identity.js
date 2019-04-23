var app = angular.module('sentinelDashboardApp');

app.controller('IdentityCtl', ['$scope', '$stateParams', 'IdentityService',
  'ngDialog', 'FlowServiceV2', 'AuthorityRuleServiceV2', 'MachineService',
  '$interval', '$location', '$timeout',
  function ($scope, $stateParams, IdentityService, ngDialog,
    FlowServiceV2, AuthorityRuleServiceV2, MachineService, $interval, $location, $timeout) {

    $scope.app = $stateParams.app;

    $scope.currentPage = 1;
    $scope.pageSize = 16;
    $scope.totalPage = 1;
    $scope.totalCount = 0;
    $scope.identities = [];
    // 数据自动刷新频率, 默认10s
    var DATA_REFRESH_INTERVAL = 30;

    $scope.isExpand = true;
    $scope.searchKey = '';
    $scope.firstExpandAll = false;
    $scope.isTreeView = true;

    $scope.macsInputConfig = {
      searchField: ['text', 'value'],
      persist: true,
      create: false,
      maxItems: 1,
      render: {
        item: function (data, escape) {
          return '<div>' + escape(data.text) + '</div>';
        }
      },
      onChange: function (value, oldValue) {
        $scope.macInputModel = value;
      }
    };
    $scope.table = null;

    var flowRuleDialog;
    var flowRuleDialogScope;
    $scope.addNewFlowRule = function (resource) {
      flowRuleDialogScope = $scope.$new(true);
      flowRuleDialogScope.currentRule = {
        enable: false,
        strategy: 0,
        grade: 1,
        controlBehavior: 0,
        resource: resource,
        limitApp: 'default',
        clusterMode: false,
        clusterConfig: {
            thresholdType: 0
        },
        app: $scope.app,
      };

      flowRuleDialogScope.flowRuleDialog = {
        title: '新增流控规则',
        type: 'add',
        confirmBtnText: '新增',
        saveAndContinueBtnText: '新增并继续添加',
        showAdvanceButton: true
      };
      // $scope.flowRuleDialog = {
      //     showAdvanceButton : true
      // };
      flowRuleDialogScope.saveRule = saveFlowRule;
      flowRuleDialogScope.saveRuleAndContinue = saveFlowRuleAndContinue;
      flowRuleDialogScope.onOpenAdvanceClick = function () {
        flowRuleDialogScope.flowRuleDialog.showAdvanceButton = false;
      };
      flowRuleDialogScope.onCloseAdvanceClick = function () {
        flowRuleDialogScope.flowRuleDialog.showAdvanceButton = true;
      };

      flowRuleDialog = ngDialog.open({
        template: '/app/views/dialog/flow-rule-dialog.html',
        width: 680,
        overlay: true,
        scope: flowRuleDialogScope
      });
    };

    function saveFlowRule() {
      if (!FlowServiceV2.checkRuleValid(flowRuleDialogScope.currentRule)) {
        return;
      }
      FlowServiceV2.newRule(flowRuleDialogScope.currentRule).success(function (data) {
        if (data.code === 0) {
          flowRuleDialog.close();
          let url = '/dashboard/v2/flow/' + $scope.app;
          $location.path(url);
        } else {
          alert('失败!');
        }
      }).error((data, header, config, status) => {
          alert('未知错误');
      });
    }

    function saveFlowRuleAndContinue() {
        if (!FlowServiceV2.checkRuleValid(flowRuleDialogScope.currentRule)) {
            return;
        }
      FlowServiceV2.newRule(flowRuleDialogScope.currentRule).success(function (data) {
        if (data.code === 0) {
          flowRuleDialog.close();
        } else {
          alert('失败!');
        }
      });
    }

      let authorityRuleDialog;
      let authorityRuleDialogScope;

      function saveAuthorityRule() {
          let ruleEntity = authorityRuleDialogScope.currentRule;
          if (!AuthorityRuleServiceV2.checkRuleValid(ruleEntity.rule)) {
              return;
          }
          AuthorityRuleServiceV2.addNewRule(ruleEntity).success((data) => {
              if (data.success) {
                  authorityRuleDialog.close();
                  let url = '/dashboard/v2/authority/' + $scope.app;
                  $location.path(url);
              } else {
                  alert('添加规则失败：' + data.msg);
              }
          }).error((data) => {
              if (data) {
                  alert('添加规则失败：' + data.msg);
              } else {
                  alert("添加规则失败：未知错误");
              }
          });
      }

      function saveAuthorityRuleAndContinue() {
          let ruleEntity = authorityRuleDialogScope.currentRule;
          if (!AuthorityRuleServiceV2.checkRuleValid(ruleEntity.rule)) {
              return;
          }
          AuthorityRuleServiceV2.addNewRule(ruleEntity).success((data) => {
              if (data.success) {
                  authorityRuleDialog.close();
              } else {
                  alert('添加规则失败：' + data.msg);
              }
          }).error((data) => {
              if (data) {
                  alert('添加规则失败：' + data.msg);
              } else {
                  alert("添加规则失败：未知错误");
              }
          });
      }

      $scope.addNewAuthorityRule = function (resource) {
          authorityRuleDialogScope = $scope.$new(true);
          authorityRuleDialogScope.currentRule = {
              app: $scope.app,
              rule: {
                  resource: resource,
                  strategy: 0,
                  limitApp: '',
              }
          };

          authorityRuleDialogScope.authorityRuleDialog = {
              title: '新增授权规则',
              type: 'add',
              confirmBtnText: '新增',
              saveAndContinueBtnText: '新增并继续添加'
          };
          authorityRuleDialogScope.saveRule = saveAuthorityRule;
          authorityRuleDialogScope.saveRuleAndContinue = saveAuthorityRuleAndContinue;

          authorityRuleDialog = ngDialog.open({
              template: '/app/views/dialog/authority-rule-dialog.html',
              width: 680,
              overlay: true,
              scope: authorityRuleDialogScope
          });
      };


    var searchHandler;
    $scope.searchChange = function (searchKey) {
      $timeout.cancel(searchHandler);
      searchHandler = $timeout(function () {
        $scope.searchKey = searchKey;
        $scope.isExpand = true;
        $scope.firstExpandAll = true;
        reInitIdentityDatas();
        $scope.firstExpandAll = false;
      }, 600);
    };

    $scope.initTreeTable = function () {
      // if (!$scope.table) {
        com_github_culmat_jsTreeTable.register(window);
        $scope.table = window.treeTable($('#identities'));
      // }
    };

    $scope.expandAll = function () {
      $scope.isExpand = true;
    };
    $scope.collapseAll = function () {
      $scope.isExpand = false;
    };
    $scope.treeView = function () {
      $scope.isTreeView = true;
      queryIdentities();
    };
    $scope.listView = function () {
      $scope.isTreeView = false;
      queryIdentities();
    };

    function queryAppMachines() {
      MachineService.getAppMachines($scope.app).success(
        function (data) {
          if (data.code === 0) {
            if (data.data) {
              $scope.machines = [];
              $scope.macsInputOptions = [];
              data.data.forEach(function (item) {
                if (item.healthy) {
                  $scope.macsInputOptions.push({
                    text: item.ip + ':' + item.port,
                    value: item.ip + ':' + item.port
                  });
                }
              });
            }
            if ($scope.macsInputOptions.length > 0) {
              $scope.macInputModel = $scope.macsInputOptions[0].value;
            }
          } else {
            $scope.macsInputOptions = [];
          }
        }
      );
    }

    // Fetch all machines by current app name.
    queryAppMachines();

    $scope.$watch('macInputModel', function () {
      if ($scope.macInputModel) {
        reInitIdentityDatas();
      }
    });

    $scope.$on('$destroy', function () {
      $interval.cancel(intervalId);
    });

    var intervalId;
    function reInitIdentityDatas() {
      // $interval.cancel(intervalId);
      queryIdentities();
      // intervalId = $interval(function () {
      //    queryIdentities();
      // }, DATA_REFRESH_INTERVAL * 1000);
    }

    function queryIdentities() {
      var mac = $scope.macInputModel.split(':');
      if (mac == null || mac.length < 2) {
        return;
      }
      if ($scope.isTreeView) {
        IdentityService.fetchIdentityOfMachine(mac[0], mac[1], $scope.searchKey).success(
          function (data) {
            if (data.code === 0 && data.data) {
              $scope.identities = data.data;
              $scope.totalCount = $scope.identities.length;
            } else {
              $scope.identities = [];
              $scope.totalCount = 0;
            }
          }
        );
      } else {
        IdentityService.fetchClusterNodeOfMachine(mac[0], mac[1], $scope.searchKey).success(
          function (data) {
            if (data.code === 0 && data.data) {
              $scope.identities = data.data;
              $scope.totalCount = $scope.identities.length;
            } else {
              $scope.identities = [];
              $scope.totalCount = 0;
            }
          }
        );
      }
    }
    $scope.queryIdentities = queryIdentities;
  }]);
